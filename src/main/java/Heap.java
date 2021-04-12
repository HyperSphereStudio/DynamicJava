import java.util.HashMap;

public class Heap {

    public ClassicHeap parentCHeap;
    final ClassicHeap cHeap = new ClassicHeap();

    public Heap(){

    }

    public Heap(ClassicHeap parentHeap){
        parentCHeap = parentHeap;
    }


    public Object get(String name) throws Exception {
        Object o = cHeap.getClassic(name);
        if(o == null)o = parentCHeap.getClassic(name);
        return o;
    }

    public void put(String name, Object o){
        cHeap.putClassic(name, o);
    }

    public void clearClassic(){
        cHeap.clear();
    }

    public void clear(){
        cHeap.clear();
    }

}

class ClassicHeap{
    private final HashMap<String, Object> map = new HashMap<>();
    public ClassicHeap(){
    }
    public void putClassic(String name, Object o){
        map.put(name, o);
    }
    public Object getClassic(String name) throws Exception {
        return map.get(name);
    }
    public void clear(){
        map.clear();
    }
}
