public class SyReCREALCompiler {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Wrong Number of arguments!\nExpected syrec path and output folder name");
            System.exit(-1);
        }

        String inSyrecFileNamePath = args[0];
        String outExportLocationPath = args[1];

        Scanner scanner = new Scanner(inSyrecFileNamePath);
        Parser parser = new Parser(scanner);
        parser.Parse(inSyrecFileNamePath, outExportLocationPath);
        System.out.println(parser.errors.count + " errors detected");
        System.exit(0);
    }
}
