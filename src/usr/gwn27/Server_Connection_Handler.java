package usr.gwn27;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class Server_Connection_Handler {

    private final SocketChannel client_channel;
    public Server_Connection_Handler(SocketChannel client_connected){
        this.client_channel = client_connected;
    }

    public void send_response(ByteBuffer next_command) throws IOException {
        ByteBuffer command_length = ByteBuffer.allocate(4);
        command_length.putInt(next_command.array().length);
        while(next_command.hasRemaining()){
            client_channel.write(request_concat(command_length, next_command));
        }
    }

    private ByteBuffer request_concat(final ByteBuffer ...buffers) {
        final ByteBuffer combined = ByteBuffer.allocate(Arrays.stream(buffers).mapToInt(Buffer::remaining).sum());
        Arrays.stream(buffers).forEach(b -> combined.put(b.duplicate()));
        return combined;
    }


    public String receive_request(){
        try{
            StringBuilder response = new StringBuilder();
            ByteBuffer message_length = ByteBuffer.allocate(4);
            AtomicInteger bytes_total = new AtomicInteger();
            fill_buffer(message_length, bytes_total);

            ByteBuffer message = ByteBuffer.allocate(bytes_total.get());
            fill_buffer(message, response);
            return new String(message.array());
        } catch (IOException e) {
            System.out.println("Error! Cannot receive data from client!");
            System.exit(0);
            return null;
        }
    }

    private <T> void fill_buffer(ByteBuffer buffer, T data_string) throws IOException {
        int read;
        while((read = client_channel.read(buffer)) != 0){
            client_channel.configureBlocking(false);
            if(data_string instanceof AtomicInteger){
                ((AtomicInteger) data_string).set(buffer.getInt());
            }else{
                ((StringBuilder) data_string).append(new String(buffer.array()), 0, read);
            }
            buffer.clear();
        }
        client_channel.configureBlocking(true);
    }
}
