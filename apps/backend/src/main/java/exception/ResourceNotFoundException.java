package exception;


//so also it's important for me so I can see exactly what the exception is being thrown, and having that exception being handled
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
}