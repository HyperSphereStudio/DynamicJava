import org.redfx.strange.local.StoredQuantumExecutor;

import java.lang.reflect.Method;

public abstract class Expression {

    private Expression(){}
    protected abstract Object invoke(Heap heap) throws Exception;
    public static class ParameterExpression extends VariableExpression{
        private ParameterExpression(Class type){
            super(type);
        }
        @Override public Object invoke(Heap heap) throws Exception { return heap.get(label); }
    }
    public static ParameterExpression parameter(Class c){
        return new ParameterExpression(c);
    }
    public static class ConstantExpression<T> extends Expression{
        private final Object o;
        public ConstantExpression(T o){
            this.o = o;
        }

        @Override
        public Object invoke(Heap heap) throws Exception {
            return o;
        }
    }

    public static abstract class VariableExpression extends Expression{
        String label;
        Class type;
        private VariableExpression(Class type){
            label = type.getSimpleName() + " " + Math.random();
            this.type = getWrappedClass(type);
        }
    }
    public static abstract class ObjectOperationExpression extends Expression{
        VariableExpression e;
        private ObjectOperationExpression(VariableExpression e){
            this.e = e;
        }
    }
    public static class GetExpression extends ObjectOperationExpression{
        private GetExpression(VariableExpression e){
            super(e);
        }

        @Override
        public Object invoke(Heap heap) throws Exception {
            return heap.get(e.label);
        }
    }
    public static class SetExpression extends ObjectOperationExpression{
        Expression value;
        private SetExpression(VariableExpression e, Expression setTo) {
            super(e);
            value = setTo;
        }

        @Override
        public Object invoke(Heap heap) throws Exception {
            Object o = value.invoke(heap);
            heap.put(e.label, o);
            return o;
        }
    }
    public static GetExpression get(VariableExpression e){
        return new GetExpression(e);
    }
    public static SetExpression set(VariableExpression e, Expression setTo){
        return new SetExpression(e, setTo);
    }

    public static abstract class ComparisonExpression extends Expression{
        Expression instanceOne, instanceTwo;
        private ComparisonExpression(Expression instanceOne, Expression instanceTwo){
            this.instanceOne = instanceOne;
            this.instanceTwo = instanceTwo;
        }
        public abstract boolean compare(Object d1, Object d2);

        @Override
        public Object invoke(Heap heap) throws Exception {
            return compare(instanceOne.invoke(heap), instanceTwo != null ? instanceTwo.invoke(heap) : null);
        }
    }
    public static ComparisonExpression isTrue(Expression instance){
        return new ComparisonExpression(instance, null) {
            @Override
            public boolean compare(Object d1, Object d2) {
                return (boolean) d1;
            }
        };
    }
    public static ComparisonExpression notIsTrue(Expression instance){
        return new ComparisonExpression(instance, null) {
            @Override
            public boolean compare(Object d1, Object d2) {
                return !(boolean) d1;
            }
        };
    }
    public static ComparisonExpression equalTo(Expression instanceOne, Expression instanceTwo){
        return new ComparisonExpression(instanceOne, instanceTwo) {
            @Override
            public boolean compare(Object o1, Object o2) {
                return o1 == o2;
            }
        };
    }
    public static ComparisonExpression notEqualTo(Expression instanceOne, Expression instanceTwo){
        return new ComparisonExpression(instanceOne, instanceTwo) {
            @Override
            public boolean compare(Object o, Object o2) {
                return o != o2;
            }
        };
    }
    public static abstract class NumberComparisonExpression extends ComparisonExpression{
        private NumberComparisonExpression(Expression instanceOne, Expression instanceTwo){
            super(instanceOne, instanceTwo);
        }
        public abstract boolean compare(double d1, double d2);
        public boolean compare(Object o1, Object o2){
            return compare(convertNumericObject(o1), convertNumericObject(o2));
        }
    }
    public static NumberComparisonExpression greaterThen(Expression instanceOne, Expression instanceTwo){
        return new NumberComparisonExpression(instanceOne, instanceTwo) {
            @Override
            public boolean compare(double d1, double d2) {
                return d1 > d2;
            }
        };
    }
    public static NumberComparisonExpression lessThen(Expression instanceOne, Expression instanceTwo){
        return new NumberComparisonExpression(instanceOne, instanceTwo) {
            @Override
            public boolean compare(double d1, double d2) {
                return d1 < d2;
            }
        };
    }

    public static abstract class ObjectExpression extends Expression{
        Expression[] parameters;
        Expression instance;
        Object[] buffer;
        public ObjectExpression(Expression instance, Expression... instances) {
            this.instance = instance;
            this.parameters = instances;
            buffer = new Object[instances.length];
        }
        public abstract Object Invoke(Object instance, Object... o);
        @Override
        protected Object invoke(Heap heap) throws Exception {
            Object o = instance.invoke(heap);
            for(int i = 0; i < parameters.length; ++i){
                buffer[i] = parameters[i].invoke(heap);
            }
            return Invoke(o, buffer);
        }
    }
    public static ObjectExpression hashCode(Expression instance){
        return new ObjectExpression(instance) {
            @Override
            public Object Invoke(Object instance, Object... o) {
                return instance.hashCode();
            }
        };
    }
    public static ObjectExpression toString(Expression instance){
        return new ObjectExpression(instance) {
            @Override
            public Object Invoke(Object instance, Object... o) {
                return instance.toString();
            }
        };
    }
    public static ObjectExpression not(Expression instance){
        return new ObjectExpression(instance) {
            @Override
            public Object Invoke(Object instance, Object... o) {
                return !(boolean) instance;
            }
        };
    }

    public static abstract class NumberOperationExpression extends ObjectExpression{
        double[] buffer;
        public NumberOperationExpression(Expression instance, Expression... instances) {
            super(instance, instances);
            buffer = new double[instances.length];
        }
        @Override
        public Object Invoke(Object instance, Object... o) {
            for(int i = 0; i < o.length; ++i){
                buffer[i] = convertNumericObject(o[i]);
            }
            return unconvertNumericObject(Invoke(convertNumericObject(instance), buffer), instance.getClass());
        }
        public abstract double Invoke(double instance, double...instances);
    }

    public static NumberOperationExpression increment(Expression instance){
        return new NumberOperationExpression(instance) {
            @Override
            public double Invoke(double instance, double... instances) {
                return instance + 1;
            }
        };
    }
    public static NumberOperationExpression decrement(Expression instance){
        return new NumberOperationExpression(instance) {
            @Override
            public double Invoke(double instance, double... instances) {
                return instance - 1;
            }
        };
    }
    public static NumberOperationExpression add(Expression instance, Expression... terms){
        return new NumberOperationExpression(instance, terms){
            @Override
            public double Invoke(double instance, double... instances) {
                for(double d : instances)instance += d;
                return instance;
            }
        };
    }
    public static NumberOperationExpression sub(Expression instance, Expression... terms){
        return new NumberOperationExpression(instance, terms){
            @Override
            public double Invoke(double instance, double... instances) {
                for(double d : instances)instance -= d;
                return instance;
            }
        };
    }
    public static NumberOperationExpression mul(Expression instance, Expression... terms){
        return new NumberOperationExpression(instance, terms){
            @Override
            public double Invoke(double instance, double... instances) {
                for(double d : instances)instance *= d;
                return instance;
            }
        };
    }
    public static NumberOperationExpression div(Expression instance, Expression... terms){
        return new NumberOperationExpression(instance, terms){
            @Override
            public double Invoke(double instance, double... instances) {
                for(double d : instances)instance /= d;
                return instance;
            }
        };
    }
    public static NumberOperationExpression mod(Expression instance, Expression div){
        return new NumberOperationExpression(instance, div){
            @Override
            public double Invoke(double instance, double... instances) {
                return instance % instances[0];
            }
        };
    }
    public static class BlockExpression extends Expression {
        Expression[] expressions;
        public BlockExpression(Expression... block){
            expressions = block;
        }
        public Object Invoke(Heap heap) throws Exception {
            for(Expression e : expressions){
                e.invoke(heap);
            }
            return null;
        }
        @Override public Object invoke(Heap heap) throws Exception {
            return Invoke(new Heap(heap.parentCHeap));
        }
    }
    public static BlockExpression block(Expression... block){
        return new BlockExpression(block);
    }
    public static class ComparisonBlockExpression extends BlockExpression{
        private ComparisonBlockExpression(Expression... block){
            super(block);
        }
    }
    public static BlockExpression ifBlock(ComparisonExpression comparisonExpression, Expression... thenBlock){
        return new ComparisonBlockExpression(thenBlock){
            @Override public Object invoke(Heap heap) throws Exception{
                if((boolean) comparisonExpression.invoke(heap)){
                    return super.invoke(heap);
                }
                return null;
            }
        };
    }

    public static class MethodCall extends Expression{
        Expression instance;
        Method m;
        Expression[] parameters;
        Object[] buffer;
        private MethodCall(Expression instance, Method m, Expression... parameters){
            this.instance = instance;
            this.m = m;
            m.setAccessible(true);
            this.parameters = parameters;
            buffer = new Object[parameters.length];
        }

        @Override
        public Object invoke(Heap heap) throws Exception {
            Object o = instance != null ? instance.invoke(heap) : null;
            for(int i = 0; i < parameters.length; ++i){
                buffer[i] = parameters[i].invoke(heap);
            }
            return m.invoke(o, buffer);
        }
    }
    public static MethodCall call(Expression instance, Method m, Expression... parameters){
        return new MethodCall(instance, m, parameters);
    }
    public static MethodCall call(Expression instance, Class c, String method, Class[] parameterTypes, Expression... parameters) throws NoSuchMethodException {
        return call(instance, c.getDeclaredMethod(method, parameterTypes), parameters);
    }
    public static MethodCall call(Method m, Expression... parameters){
        return new MethodCall(null, m, parameters);
    }
    public static class ExecutableInvocation extends Expression{
        Executable e;
        Expression[] parameters;
        Object[] buffer;
        Expression executor;
        public ExecutableInvocation(Expression executor, Executable e, Expression... parameters){
            this.e = e;
            this.parameters = parameters;
            buffer = new Object[parameters.length];
            this.executor = executor;
        }
        @Override
        public Object invoke(Heap heap) throws Exception {
            for(int i = 0; i < parameters.length; ++i){
                buffer[i] = parameters[i].invoke(heap);
            }
            return e.invoke((StoredQuantumExecutor) executor.invoke(heap), heap);
        }
    }

    public static class ReturnExpression<T> extends Expression{
        Expression instance;
        private ReturnExpression(Expression instance){
            this.instance = instance;
        }

        @Override
        public Object invoke(Heap heap) throws Exception {
            return instance.invoke(heap);
        }
    }
    public static <T> ReturnExpression<T> returnExpr(Expression e, Class<T> returnClass){
        return new ReturnExpression<>(e);
    }
    public static final class Executable<T> {
        private final Expression e;
        private final ParameterExpression[] paramClasses;
        private Executable(Expression e, ParameterExpression... parameters) {
            this.paramClasses = parameters;
            this.e = e;
        }

        public T invoke(Object... params) throws Exception {
            if(paramClasses.length != params.length)throw new Exception("Parameter Length Mismatch! Expected:" + paramClasses.length + " Received:" + params.length);
            Heap h = new Heap();
            for(int i = 0; i < paramClasses.length; ++i) {
                if(params[i].getClass() != paramClasses[i].type || params[i].getClass().isInstance(paramClasses[i].type))throw new Exception("Param Mismatch! Index:" + i + " Received:" + params[i].getClass() + " Expected:" + paramClasses[i].type);
                h.put(paramClasses[i].label, params[i]);
            }
            return (T) e.invoke(h);
        }
    }
    public static <T> Executable<T> compile(ReturnExpression<T> e, ParameterExpression... parameters){
        return new Executable(e, parameters);
    }

    public static Class getWrappedClass(Class c){
        if(c.isPrimitive()){
            if(c == int.class)return Integer.class;
            if(c == double.class)return Double.class;
            if(c == long.class)return Long.class;
            if(c == byte.class)return Byte.class;
            if(c == float.class)return Float.class;
            if(c == short.class)return Short.class;
            if(c == boolean.class)return Boolean.class;
            return null;
        }else return c;
    }
    public static double convertNumericObject(Object o){
        if(o instanceof Double)return (double)o;
        if(o instanceof Integer)return (double) (int)o;
        if(o instanceof Short)return (double) (short)o;
        if(o instanceof Byte)return (double) (byte)o;
        if(o instanceof Long)return (double) (long)o;
        if(o instanceof Float)return (double) (float)o;
        return -1;
    }
    public static Object unconvertNumericObject(double d, Class to){
        if(to == Double.class)return (d);
        if(to == Integer.class)return (int) d;
        if(to == Short.class)return (short)d;
        if(to == Byte.class)return (byte)d;
        if(to == Float.class)return (float)d;
        if(to == Long.class)return (long)d;
        return null;
    }
}
