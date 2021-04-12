# DynamicJava
 
Expression Tree for dynamic java implementation (Java's version of C#'s Expression type)

Example:


public static void main(String[] args) throws Exception {
        
        //Dynamically creates the following code
        //Created by Johnathan Bizzano
        /**
         * lambda(int param1, int param2){
         *      if(param2 > param1){
         *          test();
         *      }
         * }    
         * **/
        
        Expression.ParameterExpression param = Expression.parameter(int.class);
        Expression.ParameterExpression param2 = Expression.parameter(int.class);
        Expression e = Expression.ifBlock(Expression.greaterThen(param2, param), Expression.call(null, Test.class, "test", new Class[]{}));
        Expression.compile(Expression.returnExpr(e, int.class), param, param2).invoke(null, 1, 2);
    }

    public static void test(){
        System.out.println("Hi");
    }
 }
