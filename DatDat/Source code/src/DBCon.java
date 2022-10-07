import java.sql.*;

/*
This class is a template for objects that require a conenction to the database
*/

public abstract class DBCon {
    protected Connection con; //Connection object
    protected Boolean isConnected = false; //bool to check if the database is connected

    //Attempts to establish a connection to the database
    public void connect(){
        if(!isConnected){
            try{
                Class.forName("com.mysql.cj.jdbc.Driver");
                
                con = DriverManager.getConnection("jdbc:mysql://mysql.stud.ntnu.no/saraew_DatDatP", "saraew_user1","DatabaserErGøy!1000");
                
                isConnected = true;
                }   
            catch(Exception e){
                throw new RuntimeException("unable to connect",e);
                }
        }
        else{
            System.out.println("Already connected.");
            }
    }

    //Closes the connection
    public void disconnect(){
        if(isConnected){
            try{
                con.close();
                isConnected = false;
                }
            catch(Exception e){
                throw new RuntimeException("Disconnection failed ", e);
                }
            }
        else{
            System.out.println("Already disconnected");
            }
        }
}
