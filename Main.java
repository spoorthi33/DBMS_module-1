import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter Attributes seperated by commas");
        String attributes = scanner.nextLine();
        System.out.println("Enter FDs in AB->CD,B->A,... format");
        String fds = scanner.nextLine();
        scanner.close();

        CKFinder finder = new CKFinder(attributes, fds);

        // find candidate keys
        Set<Set<String>> ck = finder.findCK();
        System.out.println("Candidate keys: " + ck);

        // find highest normal form satisfied
        int highestNF = finder.findHighestNF(ck);
        System.out.println("highest Normal Form: " + highestNF);

        List<Map<Set<String>, Set<String>>> decomposed = new ArrayList<Map<Set<String>, Set<String>>>();
        switch (highestNF) {
            case 1:
                decomposed = finder.to2nf(ck);
                break;
            case 2:
                decomposed = finder.to3nf(ck);
                break;
            case 3:
                decomposed = finder.toBCNF(ck);
                break;
            default:
                break;
        }

        Map<Set<String>, Set<String>> aFds = decomposed.get(0);
        Map<Set<String>, Set<String>> bFds = decomposed.get(1);

        Set<String> aAttributes = finder.getAttribsInFDs(aFds);
        Set<String> bAttributes = finder.getAttribsInFDs(bFds);
        CKFinder afinder = new CKFinder(aAttributes, aFds);
        CKFinder bfinder = new CKFinder(bAttributes, bFds);

        System.out.println(String.format("\nDecomposed to %s NF :", highestNF + 1));
        System.out.println(decomposed.get(0));
        System.out.println("KEYS: " + afinder.findCK());
        System.out.println("");
        System.out.println(decomposed.get(1));
        System.out.println("KEYS: " + bfinder.findCK());
    }
}