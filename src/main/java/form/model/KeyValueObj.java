package form.model;


public  class KeyValueObj {
    private final String key;
    private final Object value;
    public   KeyValueObj(String key,Object value){
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.getKey()+ ":" + this.getValue();
    }
}