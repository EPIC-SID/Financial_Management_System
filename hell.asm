section .data

%macro Print 2
   mov rax,1
   mov rdi,1
   mov rsi,%1
   mov rdx,%2
   syscall
%endmacro

%macro Read 2
   mov rax,0
   mov rdi,0
   mov rsi,%1
   mov rdx,%2
   syscall
%endmacro

%macro Exit 0
   mov rax,60
   mov rdi,0
   syscall
%endmacro



section .bss
buffer resb 20

section .text
global _start
_start:
   Read buffer , 20
   Print msg , 20
   Exit 
   
	
