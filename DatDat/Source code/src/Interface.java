import java.sql.*;
import java.text.DateFormat;  
import java.text.SimpleDateFormat;  
import java.util.Date;
import java.util.Scanner;
import java.util.Calendar;  
    
//This is the class that will handle functions with queries to the database
public class Interface extends DBCon {
    protected PreparedStatement preStat;

    //Constructor. Connect on construction
    public Interface(){
        connect(); //Connects the database
    }

    //Function that returns datetime on a valid format
    private String getDate(){
        Date time = Calendar.getInstance().getTime(); //Obtain the current time
        DateFormat datfor = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss"); //Make format
        return datfor.format(time); //Return formated string
    }
    
    //Logs the user in
    public User login(String loginEmail, String password){
        User newUser = new User(); //Make an empty user-class for returning
        try{
        //Prepare the query
        preStat = con.prepareStatement("select * from UserProfile where Email=(?) and UserPassword=(?)");
        preStat.setString(1, loginEmail);
        preStat.setString(2, password);

        //Execute query, save the table
        ResultSet rs = preStat.executeQuery();

        if(rs.next()){ //rs.next() moves pointer in table and returns true if it is able to. If we get a match we log in
            //Found user, copy over variables and get username from database. Wont save password
            newUser.username = rs.getString("UserName");
            newUser.email = loginEmail;
            newUser.loggedIn = true;
            System.out.println("User " + rs.getString("Username") + " logged in.");
            }
        else {
            //No user was found for given login info. User-object becomes a useless object
            System.out.println("No user found");
            }
        }
        catch(Exception e){ //Catch error if something fails
        throw new RuntimeException("Could not fetch login data", e);
        }   
        return newUser; //Return the user
    }

    //Makes a new thread and first post in said thread. Returns postID
    private int[] makeThread(String content, String title, int folderID, String tag, User user){
        int[] curthreadID= new int[2]; //make an empty variable for returning
        try{
            //First we need to find which ThreadID is avaliable
            preStat =  con.prepareStatement("select count(*) as c from Thread");
            ResultSet rs = preStat.executeQuery();
            rs.next();
            curthreadID[0] = rs.getInt("c");
            
            //Prepare insert statement and execute
            preStat = con.prepareStatement("insert into Thread values((?),(?))");
            preStat.setInt(1, ++curthreadID[0]); //Increment first
            preStat.setString(2, title);
            preStat.executeUpdate();

            //Insert post + userPost relation
            curthreadID[1] = makePost(curthreadID[0], content, tag, user);

            //Another relation. This thread is under some folder
            preStat = con.prepareStatement("insert into ThreadInFolder values((?),(?))");
            preStat.setInt(1, curthreadID[0]);
            preStat.setInt(2, folderID);
            preStat.executeUpdate();

            System.out.println("Thread added with ThreadID: " + String.valueOf(curthreadID[0]));
            }
        catch(Exception e){ 
            throw new RuntimeException("Could not make thread",e);
        }
        return curthreadID;
    }

    //Makes a new post in a thread. Returns the threadID
    private int makePost(int threadID, String content, String tag, User user){
        int curpostID = 0; //Make returnvalue. Returns 0 for 
        try {
            //Get next avaliable postID
            preStat = con.prepareStatement("select count(*) as c from Post");
            ResultSet rs = preStat.executeQuery();
            rs.next();
            curpostID = rs.getInt("c");

            //Insert post 
            preStat = con.prepareStatement("insert into Post values((?),(?),(?),(?))");
            preStat.setInt(1, ++curpostID);
            preStat.setString(2, content);
            preStat.setString(3, getDate());
            
            if (tag==null) {
                preStat.setNull(4, 0);
                } 
            else{
                preStat.setString(4, tag);
                }
            preStat.executeUpdate();

            //This post was created by
            preStat = con.prepareStatement("insert into UserPost values((?),(?))");
            preStat.setInt(1, curpostID);
            preStat.setString(2, user.email);
            preStat.executeUpdate();
            
            //Add relation. This post belongs in a certain thread
            preStat = con.prepareStatement("insert into PostInThread values ((?), (?))");
            preStat.setInt(1, curpostID);
            preStat.setInt(2, threadID);
            preStat.executeUpdate();
            
            System.out.println("Post added with postID: " + String.valueOf(curpostID));
            displayPost(curpostID);

            } 
        catch (Exception e) {
            throw new RuntimeException("Could not insert post",e);
            }
    return curpostID;
    }

    //Makes a new post in the same thred as input PostID. Works as a reply
    private int replyTo(int postID, String content, String tag, User user){
        //Prepare return value
        int newPostID = 0;
        try {
            //Prepare query
            preStat = con.prepareStatement("select ThreadID from PostInThread where PostID=(?)");
            preStat.setInt(1, postID);
            ResultSet rs = preStat.executeQuery();

            if (rs.next()) {//If there is a post to reply to, make new post in corresponding thread
                newPostID = rs.getInt("ThreadID");
                makePost(newPostID, content, tag, user);
                } 
            else{
                //Could not find given post to reply to
                System.out.println("Could not find a post to reply to");
                }
            } 
        catch (Exception e) {
            throw new RuntimeException("Could not fetch data",e);
            }
        return newPostID;
    }

    //Makes a new folder in some course. Returns the folderID
    private int makeFolder(String courseID, String term, String name){
        int folderID = 0; //Make a returnvalue
        try {
            //Prepare query to find the current folderID
            preStat = con.prepareStatement("select count(*) as c from Folder");
            ResultSet rs = preStat.executeQuery();
            rs.next();
            folderID = rs.getInt("c") + 1;

            //Insert the new folder at next folderID-index
            preStat = con.prepareStatement("insert into Folder values((?),(?))");
            preStat.setInt(1, folderID);
            preStat.setString(2, name);
            preStat.executeUpdate();

            //Add relation to the course it belongs to
            preStat = con.prepareStatement("insert into FolderInCourse values((?),(?),(?))");
            preStat.setInt(1, folderID);
            preStat.setString(2, courseID);
            preStat.setString(3, term);
            preStat.executeUpdate();
            } 
        catch (Exception e) {
            throw new RuntimeException("Could not make folder",e);
            }

        return folderID;
        }

    //Makes a new course. Returns array with courseID and Term.
    private String[] makeCourse(String courseID, String term, String courseName, boolean anon, User creator){
        String[] ret = new String[1]; //Prepare return value
        try {
            //Check if the course already exists in the database
            preStat = con.prepareStatement("select * from Course where CourseID=(?) and Term=(?)");
            preStat.setString(1, courseID);
            preStat.setString(2, term);
            ResultSet rs = preStat.executeQuery();
            if (rs.next()) { //It exists, so we return a null-object
                System.out.println("This course already exists");
                return ret;
                } 
            else {
                //Insert course
                preStat = con.prepareStatement("insert into Course values((?),(?),(?),(?))");
                preStat.setString(1, courseID);
                preStat.setString(2, term);
                preStat.setString(3, courseName);
                preStat.setBoolean(4, anon);
                preStat.executeUpdate();

                //Insert user relation
                preStat = con.prepareStatement("insert into EnrolledIn values((?),(?),(?),(?),(?))");
                preStat.setString(1, creator.email);
                preStat.setNull(2,0);
                preStat.setString(3, courseID);
                preStat.setString(4, term);
                preStat.setBoolean(5, true);
                preStat.executeUpdate();
                
                //Prepare return values
                ret = new String[2];
                ret[0] = courseID;
                ret[1] = term;
                }
            } 
        catch (Exception e) {
            throw new RuntimeException("Could not create course",e);
            }
        return ret;
    }

    //Returns an array of PostIDs for posts that contain a certain keyword. 
    private int[] searchPost(String keyword, String courseID, String term){
        int[] indicies = new int[1];
        try {
            //Get the number of posts that matches the keyword
            preStat = con.prepareStatement("select count(PostID) as c from Post natural join PostInThread natural join ThreadInFolder natural join Folder natural join FolderInCourse natural join Course where Content like (?) and CourseID = (?) and Term = (?)");
            preStat.setString(1, "%"+keyword +"%");
            preStat.setString(2, courseID);
            preStat.setString(3, term);
            ResultSet rs = preStat.executeQuery();
            rs.next();
            int size = rs.getInt("c");

            //Get the valid posts
            preStat = con.prepareStatement("select PostID from Post natural join PostInThread natural join ThreadInFolder natural join Folder natural join FolderInCourse natural join Course where Content like (?) and CourseID = (?) and Term = (?)");
            preStat.setString(1, "%"+keyword +"%");
            preStat.setString(2, courseID);
            preStat.setString(3, term);
            rs = preStat.executeQuery();

            //Prepare the return array
            indicies = new int[size];
            for (int i = 0; i < size; i++) {
                rs.next();
                indicies[i] = rs.getInt("PostID");
                }            
            } 
        catch (Exception e) {
            throw new RuntimeException("Could not retrieve posts",e);
            }
        return indicies;
    }

    //Displays a post given a postID.
    private void displayPost(int postID){
        try {
            //Get the course the post belongs to.
            preStat = con.prepareStatement("select * from Post natural join PostInThread natural join ThreadInFolder natural join FolderInCourse where PostID=(?)");
            preStat.setInt(1, postID);
            ResultSet rs = preStat.executeQuery();
            String courseID = "";
            String term = "";
            if (rs.next()) {
                courseID = rs.getString("CourseID");
                term = rs.getString("Term");
            }

            //Prepare query
            preStat = con.prepareStatement("select * from Post natural join UserPost natural join UserProfile join EnrolledIn on Email=EnrolledIn.UserEmail where PostID=(?) and CourseID = (?) and Term = (?)");
            preStat.setInt(1, postID);
            preStat.setString(2, courseID);
            preStat.setString(3, term);
            
            //Execute query
            rs = preStat.executeQuery();
            if(rs.next()){

                //We add an extra string if the post has a tag.
                String extra = rs.getString("Tag");
                if (extra==null) {
                    extra="";
                }
                else{
                    extra = " Tag: " + extra;
                }
                String instructor = "";
                if (rs.getBoolean("AsInstructor")) { //If the user is an instructor, we want it to display that the post is the Instructors reply.
                    instructor = "Instructors answer";
                    
                }
                //Print the post
                System.out.println("@" + String.valueOf(postID) + ": " + rs.getString("UserName") + " posted at " + rs.getString("PostCreatedAt") + extra + "| " + instructor);
                System.out.println(rs.getString("Content"));
                System.out.println("-----------------------------------------------------------------------------------------------------------------");
                }
            else{
                System.out.println("No post for given PostID");
                }
            } 
        catch (Exception e) {
            throw new RuntimeException("Could not fetch posts",e);
            }
    }

    //Displays all the posts in a given thread belonging to a certain folder
    private void displayThread(int threadID, User user){
        try {
            //Get the parent folder and its name
            preStat = con.prepareStatement("select * from Thread natural join ThreadInFolder natural join Folder where ThreadID = (?)");
            preStat.setInt(1, threadID);
            ResultSet rs = preStat.executeQuery();
            rs.next();

            System.out.println("-----------------------------------------------------------------------------------------------------------------");
            System.out.println("Displaying Thread with title '" + rs.getString("Title") + "' belonging to folder '" + rs.getString("Foldername")+"'");
            System.out.println("-----------------------------------------------------------------------------------------------------------------");   
            
            preStat = con.prepareStatement("Select * from PostInThread natural join Post where ThreadID = (?)");
            preStat.setInt(1, threadID);
            rs = preStat.executeQuery();

            while (rs.next()){
                displayPost(rs.getInt("PostID"));
                }
            viewed(threadID, user);
            } 
        catch (Exception e) {
            throw new RuntimeException("Could not retrieve data",e);
            }


    }

    //Searches for posts containing keywords and displays them
    private void searchAndDisplay(String keyword, String courseID, String term){
        int[] str = searchPost(keyword, courseID, term);
        for (int i = 0; i < str.length; i++) {
            displayPost(str[i]);
        }
    }

    //Function that displays the stats of a given course
    private void displayStats(String courseID, String term){
        try {
            //Prepare query
            preStat = con.prepareStatement( 
                "select tab1.Email, Nviewed, Nposted " + 
            "from (select EI.UserEmail as Email, EI.CourseID, EI.Term, NViewed from EnrolledIn as EI " + 
            "left join " + 
                "(select temp.Email, temp.CourseID, temp.Term, temp.ThreadID, count(ThreadID) as Nviewed " + 
                "from " + 
                    "(select Email, CourseID, Term, ThreadID " + 
                        "from FolderInCourse " + 
                        "natural join ThreadInFolder " + 
                        "natural join Viewed where CourseID = (?) and Term = (?)) " + 
                    "as temp group by temp.CourseID, temp.Term, temp.ThreadID, temp.email) as tab1 " + 
                "on EI.UserEmail = tab1.Email and EI.CourseID = tab1.CourseID and EI.Term = tab1.Term) as tab1 " + 
            "left join " + 
            "(select temp.Email, temp.CourseID, temp.Term, count(PostID) as Nposted " + 
            "from (select Email, CourseID, Term, PostID " + 
                "from FolderInCourse natural join ThreadInFolder " + 
                "natural join Thread natural join PostInThread " + 
                "natural join Post natural join UserPost where CourseID = (?) and Term = (?)) as temp " + 
                "group by temp.CourseID, temp.email, temp.term) as tab2 " + 
            "on tab1.CourseID = tab2.CourseID and tab1.Email=tab2.Email " + 
            "where tab1.CourseID = (?) and tab1.Term = (?)" + 
            "order by Nviewed desc"
            );
            preStat.setString(1, courseID);
            preStat.setString(2, term);
            preStat.setString(3, courseID);
            preStat.setString(4, term);
            preStat.setString(5, courseID);
            preStat.setString(6, term);

            //Execute query
            ResultSet rs = preStat.executeQuery();

            //Prepare printout format
            System.out.println("-----------------------------------------------------------------------------------------------------------------");
            System.out.println("Statistics for Course " + courseID + ", " + term);
            System.out.println("-----------------------------------------------------------------------------------------------------------------");
            String ourFormat = " %10s %-30s %10s %20s %10s %20s";
            System.out.println(String.format(ourFormat,"", "Email", "|", "Posts read", "|" , "Posts created"));
            System.out.println("-----------------------------------------------------------------------------------------------------------------");

            //Print out data
            while (rs.next()){

                //Handle null-values
                Integer viewed = rs.getInt("Nviewed");
                if (viewed == null) { viewed = 0; }

                Integer posted = rs.getInt("Nposted");
                if(posted == null){posted = 0; }

                //Print stats for a given user
                System.out.println(String.format(ourFormat, "",rs.getString("Email"), "|", String.valueOf(viewed), "|",String.valueOf(posted)));
            }
            System.out.println("-----------------------------------------------------------------------------------------------------------------");
            } 
        catch (Exception e){
            throw new RuntimeException("Could not fetch data",e);
            }
    }

    //Returns a 2d array of strings with courses/terms
    private String[][] getCourses(User user){
        String[][] courses = new String[1][1];
        try {
            preStat = con.prepareStatement("select count(*) as c from Course natural join EnrolledIn where EnrolledIn.UserEmail = (?)");
            preStat.setString(1, user.email);
            ResultSet rs = preStat.executeQuery();
            rs.next();
            int size = rs.getInt("c");
            courses = new String[size][4];

            preStat = con.prepareStatement("select * from Course natural join EnrolledIn where EnrolledIn.UserEmail = (?)");
            preStat.setString(1, user.email);
            rs = preStat.executeQuery();
            for(int i = 0; i < size; i++){
                rs.next();
                courses[i][0] = rs.getString("CourseID");
                courses[i][1] = rs.getString("Term");
                courses[i][2] = rs.getString("AsInstructor");
                courses[i][3] = rs.getString("CourseName");
                }
            } 
        catch (Exception e) {
            throw new RuntimeException("Could not fetch courses",e);
        }
        return courses;
    }

    //Show courses
    private String[][] displayCourses(User user){
        String[][] courses = getCourses(user);
        System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------");
        String ourFormat = " %5s %-50s %10s %20s %5s %20s %5s %15s";
        System.out.println(String.format(ourFormat,"No.", "Coursename" ,"|" , "CourseID" , "|", "Term", "|", "Instructor"));
        System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------");

        for (int i = 0; i < courses.length; i++) {
            
            System.out.println(String.format( ourFormat,String.valueOf(i+1), courses[i][3], "|", courses[i][0], "|", courses[i][1], "|", courses[i][2]));
        }
        return courses;

    }

    //Displays the courses avaliable to the user and promts them to select one of them.
    public String[] selectCourse(User user){
        String[][] courses  = displayCourses(user);
        System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------");
        Scanner input = new Scanner(System.in);
        System.out.println("Which piazza would you like to enter? Type '0' to log out");
        Integer selection;
        String[] retval = new String[4];
        while (true) {
            
            try {
                System.out.print("Choose a course number: ");
                selection = Integer.parseInt(input.nextLine());
                if (selection>0 && selection<=courses.length) {
                    
                    return courses[selection-1];
                }
                else if (selection==0) {
                    user.logout();
                    
                    return retval;
                }
                else {
                    System.out.println("Invalid number, try again.");
                }
            } 
            catch (Exception e) {
                
                
                
                input.reset();
                
                
                System.out.println("Invalid input. Please enter a valid integer.");
                
            } 
        }
    }
    
    //Displays all the folders in a given course
    private String[][] displayFolders(String courseID, String term){
        String[][] folders = new String[1][1];
        try {
            preStat = con.prepareStatement("select count(*) as c from Folder natural join FolderInCourse where CourseID=(?) and Term=(?)");
            preStat.setString(1, courseID);
            preStat.setString(2, term);
            ResultSet rs = preStat.executeQuery();
            rs.next();
            int size = rs.getInt("c");

            folders = new String[size][2];
            preStat = con.prepareStatement("select * from Folder natural join FolderInCourse where CourseID=(?) and Term=(?)");
            preStat.setString(1, courseID);
            preStat.setString(2, term);
            rs = preStat.executeQuery();
            String ourFormat = "%5s %20s %5s %20s";
            System.out.println("-------------------------------------------------------------------------");
            System.out.println(String.format(ourFormat, "","Folder number","|","Folder name"));
            System.out.println("-------------------------------------------------------------------------");
            for (int i = 0; i < size; i++) {
                rs.next();
                folders[i][0] = rs.getString("FolderID");
                folders[i][1] = rs.getString("FolderName");
                System.out.println(String.format(ourFormat, "",String.valueOf(i+1),"|",folders[i][1]));
            }
            System.out.println("-------------------------------------------------------------------------");
        } 
        catch (Exception e) {
            throw new RuntimeException("Could not retrieve data",e);
        }
        return folders;
    }

    //Displays all folders in a given term and promts the user to select one of them.
    private String[] selectFolder(String courseID, String term){
        Scanner input = new Scanner(System.in);
        String[][] folders = displayFolders(courseID, term);
        
        System.out.println("Select the number of the folder to write a post in: ");
        Integer selection;
        String[] retval = new String[2];
        retval[0] = "0";
        while (true) {
            try {
                System.out.print("Choose a folder number: ");
                input.reset();
                selection = Integer.parseInt(input.nextLine());
                if (selection>0 && selection<=folders.length) {
                
                    return folders[selection-1];
                }
                else if (selection==0) {
                    return retval;
                }
                else {
                    System.out.println("Invalid number, try again.");
                }
            } 
            catch (Exception e) {
                
                selection = -1;
                input.reset();
                
                
                System.out.println("Invalid input. Please enter a valid integer.");
            } 
        

        }

    }

    //Promts the user to type in a keyword to search for in posts.
    private String selectKeyword(){
        String retval = "";
        try {
            Scanner input = new Scanner(System.in);
            System.out.print("Enter a search term: ");
            retval = input.nextLine();
        } catch (Exception e) {
            throw new RuntimeException("Could not get input",e);
        }
        return retval;

    }

    //Promts the user to type in the contents and tag for a new post.
    private String[] getContents(){
        String[] retval = new String[2];
        try {
            
            Scanner input = new Scanner(System.in);
            System.out.println("Please input for content then tag for new post.");
            
        
            System.out.print("Content: ");
            retval[0] = input.nextLine();
            System.out.println("");

            System.out.print("Tag: ");
            retval[1] = input.nextLine();
            System.out.println("-----------------------------------------------------");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return retval;
    }

    //Promts the user to type in the title for a new thread
    private String getTitle(){
    String retval="";
    
    try {
        Scanner input = new Scanner(System.in);
        System.out.println("-----------------------------------------------------");
        System.out.println("Input a title for the new thread: ");
        System.out.print("Title: ");
        retval = input.nextLine();
        System.out.println("");
        
        
    } 
    catch (Exception e) {
        throw new RuntimeException(e);
    }
    return retval;
    }

    //Displays all the threads that are in a given folder
    private String[][] displayThreads(int folderID){
        String[][] threads = new String[1][2];
        try {
           
            preStat = con.prepareStatement("select count(*) as c from Thread natural join ThreadInFolder where FolderID=(?)");
            preStat.setInt(1, folderID);
            ResultSet rs = preStat.executeQuery();
            rs.next();
            int size = rs.getInt("c");
            threads = new String[size][2];

            preStat = con.prepareStatement("select * from Thread natural join ThreadInFolder where FolderID=(?)");
            preStat.setInt(1, folderID);
            rs = preStat.executeQuery();

            String ourFormat = "%5s %20s %5s %50s";
            System.out.println("-------------------------------------------------------------------------");
            System.out.println(String.format(ourFormat, "","Thread number","|","Title"));
            System.out.println("-------------------------------------------------------------------------");



            for (int i = 0; i < size; i++) {
                rs.next();
                threads[i][0] = rs.getString("ThreadID");
                threads[i][1] = rs.getString("Title");
                System.out.println(String.format(ourFormat, "",String.valueOf(i+1),"|",threads[i][1]));
            }
            System.out.println("-------------------------------------------------------------------------");

            
        } 
        catch (Exception e) {
            throw new RuntimeException("Could not retrieve data",e);
        }
        return threads;

    }

    //Displays all the threads that are in a given folder and promts the user to select one.
    private String[] selectThread(int folderID){
        Scanner input = new Scanner(System.in);
        String[][] threads = displayThreads(folderID);
        
        System.out.println("Select the number of the thread you want to see");
        Integer selection;
        String[] retval = new String[2];
        retval[0] = "0";
        while (true) {
            try {
                System.out.print("Choose a number: ");
                input.reset();
                selection = Integer.parseInt(input.nextLine());
                if (selection>0 && selection<=threads.length) {
                    return threads[selection-1];
                }
                else if (selection==0) {
                    return retval;
                }
                else {
                    System.out.println("Invalid number, try again.");
                }
            } 
            catch (Exception e) {
                
                
                
                input.reset();
                selection = -1;
                
                System.out.println("Invalid input. Please enter a valid integer.");
            } 
        

        }

    }

    //Promts the user to select a folder to write a new thread in, then requests the user to type in title, content and tag
    private int[] inputThread(String courseID, String term, User user){
        String[] folder = selectFolder(courseID, term);
        if(Integer.parseInt(folder[0])!=0)
        {
            String title = getTitle();
            String[] conts = getContents();
            return makeThread(conts[0], title, Integer.parseInt(folder[0]), conts[1], user);
        }
        int[] temp = new int[2];
        temp[0] = 0;
        return temp;
    }

    //Promts the user to select a folder and thread to write a new post in, then asks the user to type in content and tag
    private int inputPost(String courseID, String term, User user){
        String[] folder = selectFolder(courseID, term);
        if(Integer.parseInt(folder[0])==0){return 0;}

        String[] thread = selectThread(Integer.parseInt(folder[0]));
        if (Integer.parseInt(thread[0])==0){return 0;}

        displayThread(Integer.parseInt(thread[0]),user);
        System.out.println("Type '1' to reply to this thread, anything else to cancel.");
        System.out.print("Input:");
        Scanner input = new Scanner(System.in);
        try{
            String selection = input.nextLine();
            if (Integer.parseInt(selection)==1) {
                String[] conts = getContents();
                preStat = con.prepareStatement("Select * from PostInThread natural join Post where ThreadID = (?)");
                preStat.setInt(1, Integer.parseInt(thread[0]));
                ResultSet rs = preStat.executeQuery();
                rs.next();
                System.out.println(rs.getInt("PostID"));
                
                return replyTo(rs.getInt("PostID"), conts[0], conts[1], user);
                }
            else{
                return 0;
            }
            }
            catch(Exception e){
                return 0;
                }
    }

    //Navigation menu that allows a logged in user to test all the usecases except logging in. 
    public void selectUsecase(User user, String[] course){
        //Ends funciton call if a user is not logged in
        if (!user.loggedIn) {
            return;  
        }
        
        Scanner input = new Scanner(System.in);
        Integer action=1;
        System.out.println("-----------------------------------------------------------------------------------------------------------------");

        while(action!=0){
            

            String ourFormat = "%5s %15s %5s %50s";
            System.out.println("-----------------------------------------------------------------------------------------------------------------");
            System.out.println(String.format(ourFormat, "Main menu|","Case number","|","Description"));
            System.out.println("-----------------------------------------------------------------------------------------------------------------");
            ourFormat = "%5s %20s %5s %50s";
            System.out.println(String.format(ourFormat, "","0","|","Return to course selection"));
            System.out.println(String.format(ourFormat, "","1","|","Realization"));
            System.out.println(String.format(ourFormat, "","2","|","Make thread"));
            System.out.println(String.format(ourFormat, "","3","|","Reply in thread"));
            System.out.println(String.format(ourFormat, "","4","|","Search by keyword"));
            System.out.println(String.format(ourFormat, "","5","|","Statistics"));
            System.out.println("-----------------------------------------------------------------------------------------------------------------");
            System.out.print("Select usecase: ");


            try{
                action = Integer.parseInt(input.nextLine());
                }
            catch(Exception e){
                action = 6;
            }
            switch (action) {
                case 1:
                    createRealization();
                    break;
                case 2:
                    int[] check = inputThread(course[0], course[1], user);
                    if (check[0]==0) {
                        System.out.println("Thread creation cancelled");
                    }
                    
                    break;
                case 3:
                    inputPost(course[0], course[1], user);
                    break;
                case 4:
                    String keyword = selectKeyword();
                    System.out.println("Search results for keyword: " + keyword);
                    System.out.println("-----------------------------------------------------------------------------------------------------------------");
                    searchAndDisplay(keyword, course[0], course[1]);
                    break;
                case 5:
                    if (Integer.parseInt(course[2])==1) {
                        displayStats(course[0], course[1]);
                        }
                    else
                        {
                        System.out.println("Only instructors have access to this functionality.");
                        }
                    break;
                default:
                    System.out.println("Input not valid");
                    break;
            }
        }
    }

    //Creates a user in the database.
    private User makeUser(String email, String username, String password){
        try{
            preStat = con.prepareStatement("insert into UserProfile values((?),(?),(?))");
            preStat.setString(1, email);
            preStat.setString(2, username);
            preStat.setString(3, password);
            preStat.executeUpdate();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not insert into database. Perhaps it already exists?",e);
        }
        return login(email, password);
    }

    //Adds a user to course relation to the database.
    private void enrollUser(User user, String courseID, String term, User instructor, boolean asInstructor){
        try {
            preStat = con.prepareStatement("insert into EnrolledIn values((?),(?),(?),(?),(?))");
            preStat.setString(1, user.email);
            preStat.setString(2, instructor.email);
            preStat.setString(3, courseID);
            preStat.setString(4, term);
            preStat.setBoolean(5, asInstructor);
            preStat.executeUpdate();

        }
        catch (Exception e) {
            throw new RuntimeException("Could not insert into database. Perhaps it already exists?",e);
        }

    }

    //Updates the users view-counter whenever a thread has been displayed. 
    private void viewed(int threadID, User user){
        try {
            preStat = con.prepareStatement("insert into Viewed values((?),(?),(?))");
            preStat.setInt(1, threadID);
            preStat.setString(2, user.email);
            preStat.setString(3, getDate());
            preStat.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Could not insert data",e);
        }
    }

    //Function that attempts to add a realization to the database. 
    public void createRealization(){
        try {
            //First make some users
            User roger = makeUser("bruk1@ntnu.no", "Roger Midtstraum", "101");
            User svein = makeUser("bruk2@ntnu.no", "Svein Erik Bratsberg", "202");
            User bryn = makeUser("bruk3@ntnu.no", "Brynjulf Owren", "303");
            User sunn = makeUser("bruk4@ntnu.no", "Sunniva Student", "404");
            User sim = makeUser("bruk5@ntnu.no", "Simon Student", "505");
            User sara = makeUser("bruk6@ntnu.no", "Sara Student", "606");
            User helg = makeUser("bruk7@ntnu.no", "Helge Langseth", "707");

            //Make some courses. Creator is enrolled as instructor by default
            makeCourse("TDT-4145", "V21", "Datamodellering og databasesystemer", false, roger);
            makeCourse("TMA-4120", "V21", "Introduksjon til vitenskapelige beregninger", false, bryn);
            makeCourse("TDT-4171", "H21", "Metoder i kunstig intelligens", true, helg);
            
            //Add users to the courses
            enrollUser(sunn, "TDT-4145", "V21", roger, false);
            enrollUser(sim, "TDT-4145", "V21", roger, false);
            enrollUser(svein, "TDT-4145", "V21", roger, true);
            enrollUser(sara, "TDT-4145", "V21", svein, false);
            
            enrollUser(sim, "TDT-4120", "V21", bryn, false);
            enrollUser(sunn, "TDT-4120", "V21", bryn, false);

            enrollUser(sara, "TDT-4171", "H21", helg, false);
            enrollUser(sim, "TDT-4171", "H21", helg, false);
            
            //Make some folders in the courses and some threads with replies
            int folderID = makeFolder("TDT-4145", "V21", "Exam");
            int[] thread = makeThread("Cant figure out how to solve this?", "Cont. 2018 problem 2", folderID, "Question", sunn);
            makePost(thread[0], "Try using...", "Reply", roger);
            makePost(thread[0], "I agree", "", svein);
            makeThread("Is this going to be on the exam.", "Cont. 2013 problem 4", folderID, "Exam Question", sim);

            folderID = makeFolder("TDT-4145", "V21", "General questions");
            makeThread("WAL refers to 'World armwresteling league'", "Important info", folderID, "Announcment", svein);

            makeFolder("TDT-4145", "V21", "Assignment 1");  //empty folder


            folderID = makeFolder("TMA-4120", "V21", "Project 1");
            thread = makeThread("Here are some hints for project 1", "Project 1 hints", folderID, "Hints", bryn);
            makePost(thread[0], "Thanks!", null, sim);
            makeFolder("TMA-4120", "V21", "Project 2"); //Empty folder
            makeFolder("TMA-4120", "V21", "Project 3"); //Empty folder
            folderID = makeFolder("TMA-4120", "V21", "Exam");
            makeThread("When is the exam?", "Exam data", folderID, "Exam data", sunn);


            folderID = makeFolder("TDT-4171", "H21", "Exam");
            thread = makeThread("What is wal?", "WAL", folderID, "Question", sara);
            makePost(thread[0], "Whops, wrong piazza", "mistake", sara);

            makeFolder("TDT-4171", "H21", "General questions");
            makeFolder("TDT-4171", "H21", "Announcements");

            }
        catch(Exception e){

            System.out.println("Unable to insert realization. Perhaps parts of it is already inserted?");
        }
    }
}
