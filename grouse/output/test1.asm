        Jump         $$main                    
        DLabel       $eat-location-zero        
        DataZ        8                         
        DLabel       $print-format-integer     
        DataC        37                        %% "%d"
        DataC        100                       
        DataC        0                         
        DLabel       $print-format-float       
        DataC        37                        %% "%g"
        DataC        103                       
        DataC        0                         
        DLabel       $print-format-boolean     
        DataC        37                        %% "%s"
        DataC        115                       
        DataC        0                         
        DLabel       $print-format-character   
        DataC        37                        %% "%c"
        DataC        99                        
        DataC        0                         
        DLabel       $print-format-string      
        DataC        37                        %% "%s"
        DataC        115                       
        DataC        0                         
        DLabel       $print-format-newline     
        DataC        10                        %% "\n"
        DataC        0                         
        DLabel       $print-format-separator   
        DataC        32                        %% " "
        DataC        0                         
        DLabel       $boolean-true-string      
        DataC        116                       %% "true"
        DataC        114                       
        DataC        117                       
        DataC        101                       
        DataC        0                         
        DLabel       $boolean-false-string     
        DataC        102                       %% "false"
        DataC        97                        
        DataC        108                       
        DataC        115                       
        DataC        101                       
        DataC        0                         
        DLabel       $errors-general-message   
        DataC        82                        %% "Runtime error: \n"
        DataC        117                       
        DataC        110                       
        DataC        116                       
        DataC        105                       
        DataC        109                       
        DataC        101                       
        DataC        32                        
        DataC        101                       
        DataC        114                       
        DataC        114                       
        DataC        111                       
        DataC        114                       
        DataC        58                        
        DataC        32                        
        DataC        10                        
        DataC        0                         
        Label        $$general-runtime-error   
        PushD        $errors-general-message   
        Printf                                 
        Halt                                   
        DLabel       $errors-int-divide-by-zero 
        DataC        105                       %% "integer divide by zero\n"
        DataC        110                       
        DataC        116                       
        DataC        101                       
        DataC        103                       
        DataC        101                       
        DataC        114                       
        DataC        32                        
        DataC        100                       
        DataC        105                       
        DataC        118                       
        DataC        105                       
        DataC        100                       
        DataC        101                       
        DataC        32                        
        DataC        98                        
        DataC        121                       
        DataC        32                        
        DataC        122                       
        DataC        101                       
        DataC        114                       
        DataC        111                       
        DataC        10                        
        DataC        0                         
        Label        $$int-divide-by-zero      
        PushD        $errors-int-divide-by-zero 
        Printf                                 
        Jump         $$general-runtime-error   
        DLabel       $errors-float-divide-by-zero 
        DataC        102                       %% "float divide by zero\n"
        DataC        108                       
        DataC        111                       
        DataC        97                        
        DataC        116                       
        DataC        32                        
        DataC        100                       
        DataC        105                       
        DataC        118                       
        DataC        105                       
        DataC        100                       
        DataC        101                       
        DataC        32                        
        DataC        98                        
        DataC        121                       
        DataC        32                        
        DataC        122                       
        DataC        101                       
        DataC        114                       
        DataC        111                       
        DataC        10                        
        DataC        0                         
        Label        $$float-divide-by-zero    
        PushD        $errors-float-divide-by-zero 
        Printf                                 
        Jump         $$general-runtime-error   
        DLabel       $usable-memory-start      
        DLabel       $global-memory-block      
        DataZ        12                        
        Label        $$main                    
        PushD        $global-memory-block      
        PushI        0                         
        Add                                    %% ya
        PushI        50                        
        PushI        0                         
        PushI        0                         
        Subtract                               
        Duplicate                              
        JumpFalse    -divide-by-zero-1         
        Jump         -safe-divisor－1           
        Label        -divide-by-zero-1         
        Jump         $$int-divide-by-zero      
        Label        -safe-divisor－1           
        Divide                                 
        StoreI                                 
        PushD        $global-memory-block      
        PushI        4                         
        Add                                    %% x
        PushI        123                       
        StoreI                                 
        Label        -while-2                  
        PushI        1                         
        BNegate                                
        JumpFalse    -while-end-2              
        PushD        $global-memory-block      
        PushI        8                         
        Add                                    %% ya
        PushI        1                         
        StoreI                                 
        PushD        $global-memory-block      
        PushI        8                         
        Add                                    %% ya
        PushD        $global-memory-block      
        PushI        8                         
        Add                                    %% ya
        LoadI                                  
        PushI        1                         
        Subtract                               
        StoreI                                 
        PushD        $global-memory-block      
        PushI        8                         
        Add                                    %% ya
        LoadI                                  
        PushD        $print-format-integer     
        Printf                                 
        PushD        $print-format-newline     
        Printf                                 
        Jump         -while-2                  
        Label        -while-end-2              
        PushD        $global-memory-block      
        PushI        0                         
        Add                                    %% ya
        LoadI                                  
        PushD        $print-format-integer     
        Printf                                 
        Halt                                   
