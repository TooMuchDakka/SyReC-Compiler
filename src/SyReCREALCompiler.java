import java.util.Optional;

public class SyReCREALCompiler {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Wrong Number of arguments!\nExpected syrec path and output folder name");
            System.exit(-1);
        }

        String inSyrecFileNamePath = args[0];
        String outExportLocationPath = args[1];
        Optional<String> optionalExportResultFilenamePrefix = Optional.empty();
        if (args.length > 2)
            optionalExportResultFilenamePrefix = Optional.of(args[2]);

        Scanner scanner = new Scanner(inSyrecFileNamePath);
        Parser parser = new Parser(scanner);
        parser.Parse(inSyrecFileNamePath, outExportLocationPath, optionalExportResultFilenamePrefix);
        System.out.println(parser.errors.count + " errors detected");
        System.exit(0);
    }
}
