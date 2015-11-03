Label	the-loop
PushD	loop-counter		
LoadI							
JumpFalse	the-end			
PushD	loop-counter			
LoadI							
PushI	1						
Subtract						
PushD	loop-counter			
Exchange						
StoreI							
PushD	loop-counter			
LoadI							
PushD	integer-format-string	
Printf							
PushD	newline-string			
Printf							
Jump	the-loop				
Label	the-end					
Halt
DLabel	integer-format-string
DataC	37						
DataC	100						
DataC	0						
DLabel	newline-string
DataC	10						
DataC	0						
DLabel	loop-counter
DataI	14
