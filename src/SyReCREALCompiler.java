import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SyReCREALCompiler {
    public static void main(String[] arg) {
        java.util.Scanner in = new java.util.Scanner(System.in);

        Scanner scanner = new Scanner("testfile.syrec");
        Parser parser = new Parser(scanner);
        parser.setName("testfolder");
        parser.Parse();
        System.out.println(parser.errors.count + " errors detected");



    }
}
