import org.apache.tuweni.bytes.Bytes;

public class Transaction{
    
    private String sender;
    private String receiver;
    private int value;
    private Bytes data;

    public Transaction(String sender, String receiver, int value, Byte[] data){
        this.sender = sender;
        this.receiver = receiver;
        this.value = value;
        this.data = data;
    }

    public String getSender(){
        return this.sender;
    }

    public String getReceiver(){
        return this.receiver;
    }

    public int getValue(){
        return this.value;
    }

    public Bytes getBytes(){
        return this.data;
    }

}

