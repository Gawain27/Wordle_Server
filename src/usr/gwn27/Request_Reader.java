package usr.gwn27;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class Request_Reader implements Runnable {
    private final SocketChannel client_connection;
    private final SelectionKey channel_key;
    private final Server_Connection_Handler server_conn_handler;
    private final Request_Evaluator r_evaluator;

    public Request_Reader(SelectionKey client_connection, Read_Write_Lock file_lock, int server_port, String group_ip) {
        this.channel_key = client_connection;
        this.client_connection = (SocketChannel)client_connection.channel();
        this.server_conn_handler = new Server_Connection_Handler(this.client_connection);
        this.r_evaluator = new Request_Evaluator(server_conn_handler, file_lock, server_port, group_ip, channel_key);
    }

    @Override
    public void run() {
        try {
            String command_requested = server_conn_handler.receive_request();
            if(command_requested == null){
                this.cancel_connection();
                return;
            }

            if(r_evaluator.evaluate_command(command_requested)){
                channel_key.interestOps(SelectionKey.OP_READ);
            }else{
                this.cancel_connection();
            }
        } catch (IOException e) {
           e.printStackTrace();
        }
    }

    private void cancel_connection() throws IOException {
        String user_left_connected = ((StringBuilder) channel_key.attachment()).toString();
        System.out.println("cancel connection! AAAAAAAAAAAA " +user_left_connected);
        if(!user_left_connected.equals("")){
            new Json_Handler().set_user_logged(user_left_connected, false);
        }
        this.client_connection.close();
        channel_key.cancel();
    }
}
