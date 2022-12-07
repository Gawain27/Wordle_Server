package usr.gwn27;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class Server_Connection_Handler {
    private final SocketChannel client_channel;
    public Server_Connection_Handler(SocketChannel client_connected){
        this.client_channel = client_connected;
    }

    public void send_response(ByteBuffer next_command) throws IOException {
        ByteBuffer buf = request_concat(next_command);
        buf.clear();
        client_channel.write(buf);
    }

    private ByteBuffer request_concat(ByteBuffer second) {
        return ByteBuffer.allocate(second.array().length+4).putInt(second.array().length).put(second);
    }


    public String receive_request(){
        try{
            StringBuilder response = new StringBuilder();
            ByteBuffer message_length = ByteBuffer.allocate(4);
            AtomicInteger bytes_total = new AtomicInteger();
            fill_buffer(message_length, bytes_total);
            ByteBuffer message = ByteBuffer.allocate(bytes_total.get());
            fill_buffer(message, response);
            if(bytes_total.get() == -1){
                throw new IOException();
            }
            return new String(message.array());
        } catch (IOException e) {
            return null;
        }
    }

    private <T> void fill_buffer(ByteBuffer buffer, T data_string) throws IOException {
        int read;
        while((read = client_channel.read(buffer)) != 0){
            if(data_string instanceof AtomicInteger){
                buffer.clear();
                ((AtomicInteger) data_string).set(buffer.getInt());
                return;
            }else{
                String to_append = new String(buffer.array());
                ((StringBuilder) data_string).append(to_append, 0, Math.min(to_append.length(),read));
            }
            buffer.clear();
        }
    }
}
