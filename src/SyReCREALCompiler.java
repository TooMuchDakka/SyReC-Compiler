public class SyReCREALCompiler {
    public static void main(String[] arg) {
        java.util.Scanner in = new java.util.Scanner(System.in);

        Scanner scanner = new Scanner("arb8_235.src");
        Parser parser = new Parser(scanner);
        parser.setName("testfolder2");
        parser.Parse();
        System.out.println(parser.errors.count + " errors detected");


    }
}
