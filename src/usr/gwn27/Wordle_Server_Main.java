package usr.gwn27;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.Executors.newFixedThreadPool;

public class Wordle_Server_Main {

    public static void main(String[] args) {
        AtomicBoolean stop_server = new AtomicBoolean(false);
        AtomicInteger server_port = new AtomicInteger(), word_time = new AtomicInteger();
        StringBuilder host_name = new StringBuilder(), group_ip = new StringBuilder();
        Read_Write_Lock file_lock = new Read_Write_Lock();
        set_config(server_port, word_time, host_name, group_ip);

        try(ServerSocketChannel server_channel = ServerSocketChannel.open()){
            ExecutorService request_pool = newFixedThreadPool(50);
            server_channel.bind(new InetSocketAddress(host_name.toString(), server_port.get()));
            server_channel.configureBlocking(false);
            Selector selector = Selector.open();
            server_channel.register(selector, SelectionKey.OP_ACCEPT);
            start_server_interaction(word_time.get(), file_lock, stop_server);

            while (!stop_server.get()) {

                selector.select(500);
                Set<SelectionKey> selected_keys = selector.selectedKeys();
                Iterator<SelectionKey> key_iterator = selected_keys.iterator();
                while (key_iterator.hasNext()) {
                    SelectionKey current_key = key_iterator.next();

                    if (current_key.isReadable()) {
                        request_pool.execute(new Request_Reader(current_key, selector, file_lock, server_port.get(), group_ip.toString()));
                        current_key.cancel();

                    }else if (current_key.isAcceptable()) {
                        SocketChannel client = server_channel.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                        client.finishConnect();
                    }
                    key_iterator.remove();
                }
            }
            request_pool.shutdown();
            System.out.println("Chiusura modulo server in corso...");
            while(!request_pool.awaitTermination(10, TimeUnit.SECONDS)) {
                System.out.println("Impossibile chiudere il programma. Nuovo tentativo...");
            }
            System.out.println("Programma terminato con successo!");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void set_config(AtomicInteger server_port,AtomicInteger word_time, StringBuilder host_name, StringBuilder group_ip) {
        boolean configuration_done = false;
        try (BufferedReader config_file = new BufferedReader(new FileReader("server.conf"))) {
            String setting;
            while ((setting = config_file.readLine()) != null) {
                String[] setting_param = setting.split(":");
                switch (setting_param[0]) {
                    case "server_port":
                        server_port.set(Integer.parseInt(setting_param[1]));
                        if (server_port.get() < 1024 || server_port.get() > 65535) {
                            throw new NumberFormatException();
                        }
                        configuration_done = true;
                        break;
                    case "next_word_time":
                        word_time.set(Integer.parseInt(setting_param[1]));
                        configuration_done = true;
                        break;
                    case "host_name":
                        host_name.append(setting_param[1].trim());
                        configuration_done = true;
                        break;
                    case "group_ip":
                        group_ip.append(setting_param[1].trim());
                        if (!group_ip.toString().matches(
                                "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$")) {
                            throw new UnsupportedOperationException();
                        }
                        configuration_done = true;
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
            if (!configuration_done) {
                throw new IOException();
            }
        } catch (FileNotFoundException e) {
            System.out.println(Colors.RED.get_color_code()+"Impossibile trovare server.conf - Chiusura applicazione..."+Colors.RESET.get_color_code());
            System.exit(0);
        } catch (UnsupportedOperationException e){
            System.out.println(Colors.RED.get_color_code()+"Errore parametro 'group_ip' - Chiusura applicazione..."+Colors.RESET.get_color_code());
            System.exit(0);
        } catch (NumberFormatException e) {
            System.out.println(Colors.RED.get_color_code()+"Errore formato 'server_port'/'word_time' - Chiusura applicazione..."+Colors.RESET.get_color_code());
            System.exit(0);
        } catch (IllegalArgumentException e) {
            System.out.println(Colors.RED.get_color_code()+"Parametri server.conf errati - Chiusura applicazione..."+Colors.RESET.get_color_code());
            System.exit(0);
        } catch (IOException e) {
            System.out.println(Colors.RED.get_color_code()+"Impossibile configurare il server di gioco - Chiusura applicazione..."+Colors.RESET.get_color_code());
            System.exit(0);
        }
    }

    private static void start_server_interaction(long word_time, Read_Write_Lock file_lock, AtomicBoolean stop_server){
        Thread word_selector_thread = new Thread(new Word_Selector(word_time, file_lock, stop_server));
        word_selector_thread.start();

        Thread control_thread = new Thread(new Server_Controller(word_selector_thread, stop_server));
        control_thread.start();
    }
}
