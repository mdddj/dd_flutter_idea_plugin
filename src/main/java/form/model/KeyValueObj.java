package form.model;


public  class KeyValueObj {
    private String key;
    private Object value;
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
}