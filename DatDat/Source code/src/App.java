public class App {
    public static void main(String[] args) throws Exception {
                Interface db = new Interface();
                System.out.println("----------------");
                User user = new User(db);
                System.out.println("----------------");
                while (user.loggedIn) {
                    String[] course = db.selectCourse(user);
                    db.selectUsecase(user, course);
                }
                System.out.println("Quitting program.");
    }
}



