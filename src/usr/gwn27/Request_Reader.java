package usr.gwn27;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class Request_Reader implements Runnable {
    private final SocketChannel client_connection;
    private final SelectionKey channel_key;
    private final Selector selector;
    private final Server_Connection_Handler server_conn_handler;
    private final Request_Evaluator r_evaluator;

    public Request_Reader(SelectionKey client_connection, Selector selector, Read_Write_Lock file_lock, int server_port, String group_ip) {
        this.channel_key = client_connection;
        this.client_connection = (SocketChannel)client_connection.channel();
        this.selector = selector;
        this.server_conn_handler = new Server_Connection_Handler(this.client_connection);
        this.r_evaluator = new Request_Evaluator(server_conn_handler, file_lock, server_port, group_ip);
    }

    @Override
    public void run() {
        try {
            String command_requested = server_conn_handler.receive_request();
            if(command_requested == null){
                return;
            }

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
