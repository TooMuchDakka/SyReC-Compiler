public class SyReCREALCompiler {
    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Wrong Number of arguments \nExpected syrec path and output folder name");
        }


        java.util.Scanner in = new java.util.Scanner(System.in);

        Scanner scanner = new Scanner(args[0]);
        Parser parser = new Parser(scanner);
        parser.setName(args[1]);
        parser.Parse();
        System.out.println(parser.errors.count + " errors detected");


    }
}
