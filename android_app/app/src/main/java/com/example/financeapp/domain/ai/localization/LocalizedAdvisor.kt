package com.example.financeapp.domain.ai.localization

import com.example.financeapp.domain.engine.Verdict
import com.example.financeapp.domain.model.ActionImpact
import com.example.financeapp.domain.model.ConfidenceLabel
import com.example.financeapp.domain.model.ConsolidatedView
import com.example.financeapp.domain.model.FinancialSnapshot
import com.example.financeapp.domain.model.ObservedItem
import com.example.financeapp.domain.model.PredictedItem
import com.example.financeapp.domain.model.PredictionRange
import com.example.financeapp.domain.model.RecommendedItem
import com.example.financeapp.domain.model.StructuredResponse
import kotlin.math.abs
import kotlin.math.roundToLong

enum class CopilotIntent { CASH_OVERVIEW, INCOME, SHOCK, RUNWAY, GOAL }

data class LocalizedAdvice(
    val language: NarrationLanguage,
    val intent: CopilotIntent,
    val response: StructuredResponse,
)

/** Offline three-section advice. Numbers come from snapshot / view / verdict; only the prose changes. */
object LocalizedAdvisor {

    /** ResultBundle bridge: keeps the verified engine metadata unchanged across languages. */
    fun adviseBundle(
        intent: com.example.financeapp.domain.model.CopilotIntent,
        bundle: com.example.financeapp.domain.model.ResultBundle,
        languageCode: String
    ): StructuredResponse {
        require(NarrationLanguage.fromCode(languageCode) != null)
        val local = com.example.financeapp.domain.ai.LocalAdvisor().advise(intent, bundle)
        if (languageCode == "en") return local
        fun money(value: Double) = "INR " + java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
        val cash = money(bundle.bankCash)
        val dues = money(bundle.cardDues)
        val balance = money(bundle.forecastP50)
        val risk = java.math.BigDecimal.valueOf(bundle.projectedShortfallRisk * 100)
            .setScale(1, java.math.RoundingMode.HALF_UP).toPlainString() + "%"
        val copy = when (languageCode) {
            "hi-Latn" -> listOf(
                "Demo bank cash $cash hai. Card dues $dues alag hain; abhi cash se minus nahi hue.",
                "${bundle.horizon} mein reserve se neeche jaane ka risk $risk hai. Ant mein median cash $balance hai.",
                "Koi action lene se pehle computed plan, reserve aur risk warnings dekhein.",
                "Yeh synthetic demo hai, aapke personal finances ki prediction nahi.")
            "hi-Deva" -> listOf(
                "डेमो बैंक में नकद $cash है। कार्ड का बकाया $dues अलग है; अभी नकद से नहीं घटा है।",
                "${bundle.horizon} में रिज़र्व से नीचे जाने का जोखिम $risk है। अंत में अनुमानित मध्य नकद $balance है।",
                "कोई कदम लेने से पहले गणना की गई योजना, रिज़र्व और जोखिम की चेतावनियाँ देखें।",
                "यह कृत्रिम डेमो है, आपके व्यक्तिगत वित्त का अनुमान नहीं।")
            else -> listOf(
                "डेमो बँकेत रोख $cash आहे. कार्डची थकबाकी $dues वेगळी आहे; अद्याप रोख रकमेतून वजा केलेली नाही.",
                "${bundle.horizon} मध्ये राखीव रकमेपेक्षा खाली जाण्याचा धोका $risk आहे. शेवटी अंदाजित मध्य रोख $balance आहे.",
                "कृती करण्यापूर्वी गणना केलेली योजना, राखीव रक्कम आणि धोक्याच्या सूचना तपासा.",
                "हे कृत्रिम डेमो आहे, तुमच्या वैयक्तिक आर्थिक स्थितीचा अंदाज नाही.")
        }
        return local.copy(observed = listOf(local.observed.single().copy(text = copy[0])),
            predicted = listOf(local.predicted.single().copy(text = copy[1])),
            recommended = listOf(local.recommended.single().copy(text = copy[2], tradeOff = copy[3])))
    }

    fun advise(
        language: NarrationLanguage,
        intent: CopilotIntent,
        snapshot: FinancialSnapshot,
        view: ConsolidatedView,
        verdict: Verdict,
        shockAmount: Double,
    ): LocalizedAdvice {
        require(shockAmount.isFinite() && shockAmount >= 0.0) { "shockAmount must be a non-negative number" }
        val facts = Facts.from(snapshot, view, verdict, shockAmount)
        val copy = copy(language, intent, facts)
        return LocalizedAdvice(
            language = language,
            intent = intent,
            response = StructuredResponse(
                observed = listOf(ObservedItem(copy.observed, refs(intent))),
                predicted = listOf(
                    PredictedItem(copy.predicted, facts.range, facts.confidence, horizon(intent)),
                ),
                recommended = listOf(RecommendedItem(copy.recommended, facts.impact(intent), copy.tradeOff)),
            ),
        )
    }

    private fun refs(intent: CopilotIntent) = when (intent) {
        CopilotIntent.CASH_OVERVIEW -> listOf("cash", "card_dues")
        CopilotIntent.INCOME -> listOf("income")
        CopilotIntent.SHOCK -> listOf("cash", "shock", "reserve")
        CopilotIntent.RUNWAY -> listOf("p10", "reserve")
        CopilotIntent.GOAL -> listOf("goal", "surplus")
    }

    private fun horizon(intent: CopilotIntent) = when (intent) {
        CopilotIntent.INCOME -> "next-pay"
        CopilotIntent.GOAL -> "goal"
        else -> "30d"
    }

    private fun copy(language: NarrationLanguage, intent: CopilotIntent, f: Facts): Copy = when (language) {
        NarrationLanguage.ENGLISH -> english(intent, f)
        NarrationLanguage.HINGLISH -> hinglish(intent, f)
        NarrationLanguage.HINDI -> hindi(intent, f)
        NarrationLanguage.MARATHI -> marathi(intent, f)
    }

    private fun english(intent: CopilotIntent, f: Facts) = when (intent) {
        CopilotIntent.CASH_OVERVIEW -> Copy(
            "Bank cash is ${f.cash}. Card dues ${f.cardDues} are separate and are not subtracted from cash.",
            "Lowest 30-day P10 cash is ${f.p10} on ${f.minP10Date}.",
            "Treat card dues as a bill, not as missing cash. Keep at least ${f.reserve} in reserve.",
            "Paying the card later reduces cash; it does not change today's cash.",
        )
        CopilotIntent.INCOME -> if (f.incomeKnown) Copy(
            "Salary is ${f.income}, expected on the ${f.payDay}.",
            "The next credit of ${f.income} is the main inflow in this 30-day window.",
            "Do not spend the paycheck before it arrives.",
            "A late credit shortens the runway.",
        ) else Copy(
            "Income is not declared yet.",
            "Without typical income, the 30-day P10 stays ${f.p10} on ${f.minP10Date}.",
            "Add typical income so advice can use it.",
            "Advice is weaker until income is known.",
        )
        CopilotIntent.SHOCK -> Copy(
            "A ${f.shock} bill would leave ${f.afterShock} in cash. Reserve is ${f.reserve}.",
            "After the bill, 30-day P10 is still computed from current cash ${f.cash}. Lowest P10 is ${f.p10} on ${f.minP10Date}.",
            if (f.absorbsShock) "Pay from the cash reserve. Card dues stay separate."
            else "This bill drops cash below reserve. Delay or split the payment.",
            "A shock uses cash now; it does not change card dues.",
        )
        CopilotIntent.RUNWAY -> Copy(
            if (f.tightOn == null) "You are fine for the next 30 days. Lowest P10 is ${f.p10} on ${f.minP10Date}."
            else "Tight on ${f.tightOn}." + (f.fineUntil?.let { " Fine until $it." } ?: ""),
            "P10 cash reaches ${f.p10} on ${f.minP10Date}.",
            if (f.tightOn == null) "Hold the reserve at ${f.reserve}."
            else "Cut discretionary before ${f.tightOn}.",
            "P10 is a low path, not a promise.",
        )
        CopilotIntent.GOAL -> if (f.hasGoal) Copy(
            "Goal ${f.goalName} is ${f.goalTarget}.",
            "Monthly surplus is ${f.surplus}.",
            if (f.goalOnTrack) "Stay on this surplus to fund the goal."
            else "Surplus is too small for ${f.goalTarget}. Relax the date or the amount.",
            "A higher reserve slows the goal; a lower reserve raises shortfall risk.",
        ) else Copy(
            "No goal is declared.",
            "Monthly surplus is ${f.surplus}.",
            "Set a goal amount and date.",
            "Without a goal, only the reserve constraint is active.",
        )
    }

    private fun hinglish(intent: CopilotIntent, f: Facts) = when (intent) {
        CopilotIntent.CASH_OVERVIEW -> Copy(
            "Bank cash ${f.cash} hai. Card dues ${f.cardDues} alag hain, cash mein se minus nahi hote.",
            "30-day P10 cash ${f.p10} hai, ${f.minP10Date} ko.",
            "Card dues ko bill samjho, cash nahi. Reserve kam se kam ${f.reserve} rakho.",
            "Card baad mein chukane se cash ghatta hai; aaj ka cash nahi.",
        )
        CopilotIntent.INCOME -> if (f.incomeKnown) Copy(
            "Salary ${f.income} hai, ${f.payDay} ko aani chahiye.",
            "Agle 30 din ka main inflow ${f.income} hai.",
            "Paycheck aane se pehle mat udao.",
            "Late credit se runway chhoti ho jati hai.",
        ) else Copy(
            "Income abhi declare nahi hui.",
            "Income ke bina 30-day P10 ${f.p10} hai, ${f.minP10Date} ko.",
            "Typical income add karo taaki advice use kar sake.",
            "Income pata nahi to advice kamzor hai.",
        )
        CopilotIntent.SHOCK -> Copy(
            "${f.shock} ka bill cash ${f.afterShock} chhodega. Reserve ${f.reserve} hai.",
            "Bill ke baad bhi P10 current cash ${f.cash} se nikalta hai. Lowest P10 ${f.p10} hai, ${f.minP10Date} ko.",
            if (f.absorbsShock) "Cash reserve se bharo. Card dues alag rehte hain."
            else "Yeh bill cash ko reserve se neeche le aata hai. Delay ya split karo.",
            "Shock abhi cash use karta hai; card dues nahi badalta.",
        )
        CopilotIntent.RUNWAY -> Copy(
            if (f.tightOn == null) "Agle 30 din fine ho. Lowest P10 ${f.p10} hai, ${f.minP10Date} ko."
            else "Tight ${f.tightOn} ko." + (f.fineUntil?.let { " Fine $it tak." } ?: ""),
            "P10 cash ${f.p10} tak jata hai, ${f.minP10Date} ko.",
            if (f.tightOn == null) "Reserve ${f.reserve} par rakho."
            else "${f.tightOn} se pehle discretionary kaato.",
            "P10 low path hai, promise nahi.",
        )
        CopilotIntent.GOAL -> if (f.hasGoal) Copy(
            "Goal ${f.goalName} ${f.goalTarget} hai.",
            "Mahine ka surplus ${f.surplus} hai.",
            if (f.goalOnTrack) "Is surplus par raho, goal fund ho jayega."
            else "${f.goalTarget} ke liye surplus kam hai. Date ya amount relax karo.",
            "Zyada reserve goal slow karta hai; kam reserve shortfall risk badhata hai.",
        ) else Copy(
            "Koi goal declare nahi hai.",
            "Mahine ka surplus ${f.surplus} hai.",
            "Goal amount aur date set karo.",
            "Goal na ho to sirf reserve constraint chalti hai.",
        )
    }

    private fun hindi(intent: CopilotIntent, f: Facts) = when (intent) {
        CopilotIntent.CASH_OVERVIEW -> Copy(
            "बैंक में नकद ${f.cash} है। कार्ड का बकाया ${f.cardDues} अलग है, नकद में से नहीं कटा।",
            "30 दिन का न्यूनतम P10 नकद ${f.p10} है, ${f.minP10Date} को।",
            "कार्ड बकाया को बिल मानो, नकद नहीं। रिज़र्व कम से कम ${f.reserve} रखो।",
            "कार्ड बाद में चुकाने से नकद घटेगा; आज का नकद नहीं बदलता।",
        )
        CopilotIntent.INCOME -> if (f.incomeKnown) Copy(
            "वेतन ${f.income} है, ${f.payDay} तारीख को आने की उम्मीद है।",
            "अगले 30 दिनों का मुख्य inflow ${f.income} है।",
            "वेतन आने से पहले उसे खर्च मत करो।",
            "देर से क्रेडिट से runway छोटी हो जाती है।",
        ) else Copy(
            "आय अभी घोषित नहीं है।",
            "आय के बिना 30 दिन का P10 ${f.p10} है, ${f.minP10Date} को।",
            "सामान्य आय जोड़ो ताकि सलाह उसका उपयोग कर सके।",
            "आय अज्ञात हो तो सलाह कमज़ोर रहती है।",
        )
        CopilotIntent.SHOCK -> Copy(
            "${f.shock} का बिल नकद ${f.afterShock} छोड़ेगा। रिज़र्व ${f.reserve} है।",
            "बिल के बाद भी P10 मौजूदा नकद ${f.cash} से निकलेगा। न्यूनतम P10 ${f.p10} है, ${f.minP10Date} को।",
            if (f.absorbsShock) "नकद रिज़र्व से चुकाओ। कार्ड बकाया अलग रहता है।"
            else "यह बिल नकद को रिज़र्व से नीचे ले आता है। भुगतान टालो या बाँटो।",
            "झटका अभी नकद उपयोग करता है; कार्ड बकाया नहीं बदलता।",
        )
        CopilotIntent.RUNWAY -> Copy(
            if (f.tightOn == null) "अगले 30 दिन ठीक हैं। न्यूनतम P10 ${f.p10} है, ${f.minP10Date} को।"
            else "तंगी ${f.tightOn} को।" + (f.fineUntil?.let { " $it तक ठीक।" } ?: ""),
            "P10 नकद ${f.p10} तक जाता है, ${f.minP10Date} को।",
            if (f.tightOn == null) "रिज़र्व ${f.reserve} पर रखो।"
            else "${f.tightOn} से पहले विवेकाधीन खर्च काटो।",
            "P10 निम्न पथ है, वादा नहीं।",
        )
        CopilotIntent.GOAL -> if (f.hasGoal) Copy(
            "लक्ष्य ${f.goalName} ${f.goalTarget} है।",
            "मासिक अधिशेष ${f.surplus} है।",
            if (f.goalOnTrack) "इसी अधिशेष पर रहो, लक्ष्य पूरा होगा।"
            else "${f.goalTarget} के लिए अधिशेष कम है। तारीख या राशि ढीली करो।",
            "ज़्यादा रिज़र्व लक्ष्य धीमा करता है; कम रिज़र्व कमी का जोखिम बढ़ाता है।",
        ) else Copy(
            "कोई लक्ष्य घोषित नहीं है।",
            "मासिक अधिशेष ${f.surplus} है।",
            "लक्ष्य की राशि और तारीख तय करो।",
            "लक्ष्य न हो तो केवल रिज़र्व बाध्यता चलती है।",
        )
    }

    private fun marathi(intent: CopilotIntent, f: Facts) = when (intent) {
        CopilotIntent.CASH_OVERVIEW -> Copy(
            "बँकेत रोख ${f.cash} आहे. कार्डची थकबाकी ${f.cardDues} वेगळी आहे, रोख रकमेतून वजा नाही.",
            "30 दिवसांचा किमान P10 रोख ${f.p10} आहे, ${f.minP10Date} रोजी.",
            "कार्ड थकबाकी बिल माना, रोख नाही. रिझर्व किमान ${f.reserve} ठेवा.",
            "कार्ड नंतर फेडल्याने रोख कमी होते; आजची रोख बदलत नाही.",
        )
        CopilotIntent.INCOME -> if (f.incomeKnown) Copy(
            "पगार ${f.income} आहे, ${f.payDay} तारखेला अपेक्षित आहे.",
            "पुढील 30 दिवसांचा मुख्य inflow ${f.income} आहे.",
            "पगार येण्याआधी तो खर्च करू नका.",
            "उशिरा क्रेडिटमुळे runway आखूड होते.",
        ) else Copy(
            "उत्पन्न अद्याप घोषित नाही.",
            "उत्पन्नाशिवाय 30 दिवसांचा P10 ${f.p10} आहे, ${f.minP10Date} रोजी.",
            "नेहमीचे उत्पन्न जोडा, म्हणजे सल्ला ते वापरू शकेल.",
            "उत्पन्न माहीत नसेल तर सल्ला कमकुवत असतो.",
        )
        CopilotIntent.SHOCK -> Copy(
            "${f.shock} चे बिल रोख ${f.afterShock} सोडेल. रिझर्व ${f.reserve} आहे.",
            "बिलानंतरही P10 सध्याच्या रोख ${f.cash} वरून निघेल. किमान P10 ${f.p10} आहे, ${f.minP10Date} रोजी.",
            if (f.absorbsShock) "रोख रिझर्वमधून भरा. कार्ड थकबाकी वेगळी राहते."
            else "हे बिल रोख रिझर्वखाली आणते. भुगतान तहकूब करा किंवा वाटा.",
            "धक्का आत्ता रोख वापरतो; कार्ड थकबाकी बदलत नाही.",
        )
        CopilotIntent.RUNWAY -> Copy(
            if (f.tightOn == null) "पुढील 30 दिवस ठीक आहेत. किमान P10 ${f.p10} आहे, ${f.minP10Date} रोजी."
            else "आंवट ${f.tightOn} रोजी." + (f.fineUntil?.let { " $it पर्यंत ठीक." } ?: ""),
            "P10 रोख ${f.p10} पर्यंत जातो, ${f.minP10Date} रोजी.",
            if (f.tightOn == null) "रिझर्व ${f.reserve} वर ठेवा."
            else "${f.tightOn} पूर्वी विवेकाधीन खर्च कापा.",
            "P10 निम्न मार्ग आहे, वचन नाही.",
        )
        CopilotIntent.GOAL -> if (f.hasGoal) Copy(
            "ध्येय ${f.goalName} ${f.goalTarget} आहे.",
            "मासिक शिल्लक ${f.surplus} आहे.",
            if (f.goalOnTrack) "या शिल्लकीवर राहा, ध्येय पूर्ण होईल."
            else "${f.goalTarget} साठी शिल्लक कमी आहे. तारीख किंवा रक्कम सैल करा.",
            "जास्त रिझर्व ध्येय मंद करतो; कमी रिझर्व तुटीचा धोका वाढवतो.",
        ) else Copy(
            "ध्येय घोषित नाही.",
            "मासिक शिल्लक ${f.surplus} आहे.",
            "ध्येयाची रक्कम आणि तारीख ठरवा.",
            "ध्येय नसेल तर फक्त रिझर्व बंधन लागू आहे.",
        )
    }

    private data class Copy(
        val observed: String,
        val predicted: String,
        val recommended: String,
        val tradeOff: String,
    )

    private data class Facts(
        val cash: String,
        val cardDues: String,
        val income: String,
        val payDay: String,
        val reserve: String,
        val shock: String,
        val afterShock: String,
        val p10: String,
        val minP10Date: String,
        val fineUntil: String?,
        val tightOn: String?,
        val goalName: String,
        val goalTarget: String,
        val surplus: String,
        val incomeKnown: Boolean,
        val hasGoal: Boolean,
        val absorbsShock: Boolean,
        val goalOnTrack: Boolean,
        val range: PredictionRange,
        val confidence: ConfidenceLabel,
        val monthlyOut: Double,
        val shockAmount: Double,
        val incomeAmount: Double,
        val surplusAmount: Double,
        val goalGap: Double,
        val shortfallChance: Double,
        val cashAmount: Double,
    ) {
        fun impact(intent: CopilotIntent): ActionImpact {
            val runwayMonths = if (monthlyOut > 0.0) cashAmount / monthlyOut else 0.0
            val shockMonths = if (monthlyOut > 0.0) -shockAmount / monthlyOut else 0.0
            val incomeMonths = if (monthlyOut > 0.0) incomeAmount / monthlyOut else 0.0
            val goalMonths = if (surplusAmount > 0.0 && goalGap > 0.0) goalGap / surplusAmount else 0.0
            return when (intent) {
                CopilotIntent.CASH_OVERVIEW, CopilotIntent.RUNWAY ->
                    ActionImpact(runwayMonths, shortfallChance, 0.0)
                CopilotIntent.INCOME -> ActionImpact(incomeMonths, shortfallChance, 0.0)
                CopilotIntent.SHOCK -> ActionImpact(shockMonths, shortfallChance, 0.0)
                CopilotIntent.GOAL -> ActionImpact(goalMonths, shortfallChance, 0.0)
            }
        }

        companion object {
            fun from(
                snapshot: FinancialSnapshot,
                view: ConsolidatedView,
                verdict: Verdict,
                shockAmount: Double,
            ): Facts {
                val incomeAmount = snapshot.declared.typicalIncome ?: 0.0
                val pay = snapshot.recurring.firstOrNull { it.kind == "INCOME" }
                val last = snapshot.history.lastOrNull()
                val surplusAmount = if (last == null) {
                    0.0
                } else {
                    last.income - last.essentials - last.debtService - last.discretionary - last.investments
                }
                val afterShock = view.liquidCash - shockAmount
                val goal = snapshot.declared.goal
                val goalGap = if (goal == null) 0.0 else (goal.targetAmount - snapshot.cash).coerceAtLeast(0.0)
                val remaining = goal?.byPeriodIndex?.coerceAtLeast(1) ?: 1
                val monthlyOut = view.monthlyEmis + view.monthlySip + view.avgDiscretionary
                val nextInflow = snapshot.cash + incomeAmount
                return Facts(
                    cash = inr(view.liquidCash),
                    cardDues = inr(view.cardDues),
                    income = inr(incomeAmount),
                    payDay = pay?.nextDue?.dayOfMonth?.toString() ?: "—",
                    reserve = inr(snapshot.declared.minReserve),
                    shock = inr(shockAmount),
                    afterShock = inr(afterShock),
                    p10 = inr(verdict.minP10),
                    minP10Date = verdict.minP10Date.toString(),
                    fineUntil = verdict.fineUntil?.toString(),
                    tightOn = verdict.tightOn?.toString(),
                    goalName = goal?.name ?: "",
                    goalTarget = goal?.let { inr(it.targetAmount) } ?: inr(0.0),
                    surplus = inr(surplusAmount),
                    incomeKnown = snapshot.declared.typicalIncome != null,
                    hasGoal = goal != null,
                    absorbsShock = afterShock >= snapshot.declared.minReserve,
                    goalOnTrack = goal != null && (snapshot.cash >= goal.targetAmount || surplusAmount * remaining >= goalGap),
                    range = PredictionRange(verdict.minP10, snapshot.cash, nextInflow),
                    confidence = when {
                        snapshot.completeness >= 0.8 -> ConfidenceLabel.High
                        snapshot.completeness >= 0.5 -> ConfidenceLabel.Medium
                        else -> ConfidenceLabel.Low
                    },
                    monthlyOut = monthlyOut,
                    shockAmount = shockAmount,
                    incomeAmount = incomeAmount,
                    surplusAmount = surplusAmount,
                    goalGap = goalGap,
                    shortfallChance = verdict.shortfallChance,
                    cashAmount = view.liquidCash,
                )
            }
        }
    }
}

private const val MINUS = "\u2212"
private const val RUPEE = "\u20B9"

internal fun inr(amount: Double): String {
    val sign = if (amount < 0) MINUS else ""
    val whole = abs(amount).roundToLong().toString()
    val grouped = if (whole.length <= 3) {
        whole
    } else {
        val last3 = whole.takeLast(3)
        val rest = whole.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
        "$rest,$last3"
    }
    return "$sign$RUPEE$grouped"
}
