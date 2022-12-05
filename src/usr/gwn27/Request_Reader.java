package usr.gwn27;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class Request_Reader implements Runnable {
    private final SocketChannel client_connection;
    private final SelectionKey channel_key;
    private final Selector selector;
    private final Read_Write_Lock file_lock;
    private final Server_Connection_Handler server_conn_handler;

    public Request_Reader(SelectionKey client_connection, Selector selector, Read_Write_Lock file_lock) {
        this.channel_key = client_connection;
        this.client_connection = (SocketChannel)client_connection.channel();
        this.selector = selector;
        this.file_lock = file_lock;
        this.server_conn_handler = new Server_Connection_Handler(this.client_connection);
    }

    @Override
    public void run() {
        try {
            String command_requested = server_conn_handler.receive_request();
            Request_Evaluator r_evaluator = new Request_Evaluator(server_conn_handler, file_lock);
            if(r_evaluator.evaluate_command(command_requested)){
                this.client_connection.register(this.selector, SelectionKey.OP_READ);
            }else{
                this.cancel_connection();
            }
        } catch (IOException e) {
           e.printStackTrace();
        }
    }

    private void cancel_connection() throws IOException {
        this.client_connection.close();
        channel_key.cancel();
    }
}
