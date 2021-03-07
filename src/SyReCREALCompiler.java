public class SyReCREALCompiler {
    public static void main(String[] arg) {
        java.util.Scanner in = new java.util.Scanner(System.in);

        Scanner scanner = new Scanner("Thesis.syrec");
        Parser parser = new Parser(scanner);
        parser.setName("ThesisCircuits");
        parser.Parse();
        System.out.println(parser.errors.count + " errors detected");


    }
}
