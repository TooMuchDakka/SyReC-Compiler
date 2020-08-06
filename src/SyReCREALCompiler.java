import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SyReCREALCompiler {
    public static void main(String[] arg) {
        java.util.Scanner in = new java.util.Scanner(System.in);
        System.out.println("Please specify input file\n");
        String fileName = in.nextLine();
        Scanner scanner = new Scanner(fileName);
        Parser parser = new Parser(scanner);
        parser.setName(fileName);
        parser.Parse();
        System.out.println(parser.errors.count + " errors detected");



    }
}
