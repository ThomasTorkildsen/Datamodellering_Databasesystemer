
import java.sql.*;
import java.util.Scanner;





public class User {
    //Defining variables
    protected String email;
    protected String username;
    protected Boolean loggedIn;

    //Constructor 1. Does not do anything.
    public User(){}

    //Constructor 2. Will attempt to log user onto the system
    public User(String loginEmail, String password, Interface db){
        login(loginEmail, password, db);
        }

    //Constructor 3. Promts the user to enter login credentials and logs them in through the given database.
    public User(Interface db){
        Scanner input = new Scanner(System.in);
        Scanner input2 = new Scanner(System.in);
        boolean cont = true;
        String password;
        while (cont) {
            try {
                System.out.println("Please enter an email or 'Quit' to exit");
                input.reset();
                System.out.print("Input: ");
                email = input.next();
                if(email.equals("Quit")){
                    email = null;
                    System.out.println("Quitting...");
                    
                    cont = false;
                    }
                else {
                    System.out.println("Please enter a password");
                    System.out.print("Input: ");
                    input2.reset();
                    password = input2.next();
                    login(email, password, db);
                    if (loggedIn) {
                        cont = false;
                        }
                }
                
            } 
            catch (Exception e) {
                input.reset();
                input2.reset();
            }
        }
        
        
    }
    
    //Make login a seperate funciton
    public void login(String loginEmail, String password, Interface db){
        User thisUser = db.login(loginEmail, password);
        email = thisUser.email;
        username = thisUser.email;
        loggedIn = thisUser.loggedIn;
    }
    
    //Make logout function
    public void logout(){
        email = null;
        username = null;
        loggedIn = false;
        System.out.println("Logout successfull");
    }
}

