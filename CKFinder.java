import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class CKFinder {
    Set<String> attributes = new HashSet<String>();
    Map<Set<String>, Set<String>> fds = new LinkedHashMap<>();

    public CKFinder(String attributes, String fds) {
        // parse and store attributes as set
        String[] splitAttributes = attributes.split(",");
        for (String string : splitAttributes) {
            this.attributes.add(string);
        }

        // parse and store FDs as map
        String[] splitFds = fds.split(",");
        for (String string : splitFds) {
            String[] split = string.split("->");
            String lhs = split[0];
            String rhs = split[1];

            // sort lhs because it will be used as key in FDs map
            lhs = sortString(lhs);

            // convert lhs & rhs strings to sets
            Set<String> lhsSet = new LinkedHashSet<String>();
            for (Character c : lhs.toCharArray()) {
                lhsSet.add(c.toString());
            }
            Set<String> rhsSet = new HashSet<String>();
            for (Character c : rhs.toCharArray()) {
                rhsSet.add(c.toString());
            }

            // add lhs and rhs to fds map
            this.fds.put(lhsSet, rhsSet);
        }
    }

    public CKFinder(Set<String> attributes, Map<Set<String>, Set<String>> fds) {
        this.attributes = attributes;
        this.fds = fds;
    }

    public Set<Set<String>> findCK() {
        Set<Set<String>> ck = new HashSet<Set<String>>();
        List<Set<String>> powerset = new ArrayList<Set<String>>(GetPowerSet(attributes));

        // sort the powerset in increasing order of subset size
        powerset.sort(new Comparator<Set<String>>() {
            @Override
            public int compare(Set<String> s1, Set<String> s2) {
                return s1.size() - s2.size();
            }
        });

        // iterate on powerset to find candidate keys
        while (!powerset.isEmpty()) {
            Set<String> current = powerset.get(0);
            powerset.remove(0);
            Set<String> currentClosure = getClosure(current);
            if (currentClosure.equals(attributes)) {
                ck.add(current);
                Iterator<Set<String>> iterator = powerset.iterator();
                while (iterator.hasNext()) {
                    Set<String> set = iterator.next();
                    if (set.containsAll(current)) {
                        iterator.remove();
                    }
                }
            }
        }

        return ck;
    }

    private Set<String> getClosure(Set<String> input) {
        Set<String> closureOld = new HashSet<String>(input);
        Set<String> closureNew = new HashSet<String>(input);
        while (true) {
            for (Entry<Set<String>, Set<String>> fd : fds.entrySet()) {
                // if closure set has all of the attrs in the key, but not those in the value,
                // add the attrs in value to the closure set
                if (closureNew.containsAll(fd.getKey()) && !closureNew.containsAll(fd.getValue())) {
                    closureNew.addAll(fd.getValue());
                }
            }
            if (closureNew.equals(closureOld)) {
                break;
            }
            closureOld = closureNew;
        }

        return closureNew;
    }

    private Set<Set<String>> GetPowerSet(Set<String> originalSet) {
        Set<Set<String>> sets = new HashSet<Set<String>>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<String>());
            return sets;
        }
        List<String> list = new ArrayList<String>(originalSet);
        String head = list.get(0);
        Set<String> rest = new HashSet<String>(list.subList(1, list.size()));
        for (Set<String> set : GetPowerSet(rest)) {
            Set<String> newSet = new HashSet<String>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }

    private Set<String> findKeyAttributes(Set<Set<String>> candidateKeys) {
        Set<String> keyAttributes = new HashSet<String>();
        for (Set<String> candidateKey : candidateKeys) {
            for (String key : candidateKey) {
                if (!keyAttributes.contains(key)) {
                    keyAttributes.add(key);
                }
            }
        }
        return keyAttributes;
    }

    private Set<String> findNonKeyAttributes(Set<Set<String>> candidateKeys) {
        Set<String> keyAttributes = findKeyAttributes(candidateKeys);
        Set<String> nonKeyAttributes = new HashSet<String>(attributes);
        nonKeyAttributes.removeAll(keyAttributes);
        return nonKeyAttributes;
    }

    // if keyAttributes are already known, prefer this method
    private Set<String> findNonKeyAttributes(Set<Set<String>> candidateKeys, Set<String> keyAttributes) {
        Set<String> nonKeyAttributes = new HashSet<String>(attributes);
        nonKeyAttributes.removeAll(keyAttributes);
        return nonKeyAttributes;
    }

    private Set<Set<String>> findSuperKeys(Set<Set<String>> candidateKeys) {
        // combinations of candidate keys with non-key attributes
        Set<Set<String>> superKeys = new HashSet<Set<String>>();
        Set<String> nonKeyAttributes = findNonKeyAttributes(candidateKeys);
        for (Set<String> candidateKey : candidateKeys) {
            for (String nonKeyAttribute : nonKeyAttributes) {
                Set<String> candidateKeyCopy = new HashSet<String>(candidateKey);
                candidateKeyCopy.add(nonKeyAttribute);
                superKeys.add(candidateKeyCopy);
            }
        }
        return superKeys;
    }

    public int findHighestNF(Set<Set<String>> candidateKeys) {
        // The return values 1,2,3,4 stand for 1nf,2nf,3nf,bcnf respectively

        Set<String> keyAttributes = findKeyAttributes(candidateKeys);
        Set<String> nonKeyAttributes = findNonKeyAttributes(candidateKeys, keyAttributes);
        Set<Set<String>> superKeys = findSuperKeys(candidateKeys);
        System.out.println("key attribs: " + keyAttributes);
        System.out.println("non key attribs: " + nonKeyAttributes);
        System.out.println("superKeys: " + superKeys);

        // check for 2nf
        for (Entry<Set<String>, Set<String>> fd : fds.entrySet()) {
            Boolean LhsIsPartOfCandidateKey = false;
            for (Set<String> candidateKey : candidateKeys) {
                if (candidateKey.containsAll(fd.getKey()) && !candidateKey.equals(fd.getKey())) {
                    LhsIsPartOfCandidateKey = true;
                    break;
                }
            }
            Boolean rhsContainsNonKeyAttribute = false;
            for (String rhsAttribute : fd.getValue()) {
                if (nonKeyAttributes.contains(rhsAttribute)) {
                    rhsContainsNonKeyAttribute = true;
                    break;
                }
            }
            if (LhsIsPartOfCandidateKey && rhsContainsNonKeyAttribute) {
                return 1;
            }
        }

        // check for 3nf
        for (Entry<Set<String>, Set<String>> fd : fds.entrySet()) {
            if (!superKeys.contains(fd.getKey())) {
                for (String rhsAttribute : fd.getValue()) {
                    if (!keyAttributes.contains(rhsAttribute)) {
                        return 2;
                    }
                }
            }
        }

        // check for bcnf
        for (Entry<Set<String>, Set<String>> fd : fds.entrySet()) {
            if (!superKeys.contains(fd.getKey())) {
                return 3;
            }
        }

        return 4;
    }

    public List<Map<Set<String>, Set<String>>> to2nf(Set<Set<String>> candidateKeys) {
        List<Map<Set<String>, Set<String>>> results = new ArrayList<Map<Set<String>, Set<String>>>();
        Set<String> nonKeyAttributes = findNonKeyAttributes(candidateKeys);
        Map<Set<String>, Set<String>> bFds = new LinkedHashMap<>();
        // aFds is initially a copy of fds
        Map<Set<String>, Set<String>> aFds = new LinkedHashMap<>(fds);

        for (Entry<Set<String>, Set<String>> fd : aFds.entrySet()) {
            Boolean LhsIsPartOfCandidateKey = false;
            for (Set<String> candidateKey : candidateKeys) {
                if (candidateKey.containsAll(fd.getKey()) && !candidateKey.equals(fd.getKey())) {
                    LhsIsPartOfCandidateKey = true;
                    break;
                }
            }
            if (LhsIsPartOfCandidateKey) {
                Iterator<String> iter = fd.getValue().iterator();
                while (iter.hasNext()) {
                    String rhsAttribute = iter.next();
                    if (nonKeyAttributes.contains(rhsAttribute)) {
                        iter.remove();
                        Set<String> rhsSet = new HashSet<String>();
                        rhsSet.add(rhsAttribute);
                        bFds.put(fd.getKey(), rhsSet);
                    }
                }
                // for (String rhsAttribute : fd.getValue()) {
                // if (nonKeyAttributes.contains(rhsAttribute)) {
                // fd.getValue().remove(rhsAttribute);
                // Set<String> rhsSet = new HashSet<String>();
                // rhsSet.add(rhsAttribute);
                // bFds.put(fd.getKey(), rhsSet);
                // }
                // }
            }
        }

        // remove empty valued keys from aFds
        Iterator<Set<String>> iter = aFds.keySet().iterator();
        while (iter.hasNext()) {
            Set<String> key = iter.next();
            Set<String> value = aFds.get(key);
            if (value.isEmpty()) {
                iter.remove();
            }
        }

        results.add(aFds);
        results.add(bFds);

        return results;
    }

    public List<Map<Set<String>, Set<String>>> to3nf(Set<Set<String>> candidateKeys) {
        List<Map<Set<String>, Set<String>>> results = new ArrayList<Map<Set<String>, Set<String>>>();
        Set<String> keyAttributes = findKeyAttributes(candidateKeys);
        Set<Set<String>> superKeys = findSuperKeys(candidateKeys);
        Map<Set<String>, Set<String>> bFds = new LinkedHashMap<>();
        // aFds is initially a copy of fds
        Map<Set<String>, Set<String>> aFds = new LinkedHashMap<>(fds);

        for (Entry<Set<String>, Set<String>> fd : aFds.entrySet()) {
            if (!superKeys.contains(fd.getKey())) {
                Iterator<String> iter = fd.getValue().iterator();
                while (iter.hasNext()) {
                    String rhsAttribute = iter.next();
                    if (!keyAttributes.contains(rhsAttribute)) {
                        iter.remove();
                        Set<String> rhsSet = new HashSet<String>();
                        rhsSet.add(rhsAttribute);
                        bFds.put(fd.getKey(), rhsSet);
                    }
                }
                // for (String rhsAttribute : fd.getValue()) {
                // if (!keyAttributes.contains(rhsAttribute)) {
                // fd.getValue().remove(rhsAttribute);
                // Set<String> rhsSet = new HashSet<String>();
                // rhsSet.add(rhsAttribute);
                // bFds.put(fd.getKey(), rhsSet);
                // }
                // }
            }
        }

        // remove empty valued keys from aFds
        Iterator<Set<String>> iter = aFds.keySet().iterator();
        while (iter.hasNext()) {
            Set<String> key = iter.next();
            Set<String> value = aFds.get(key);
            if (value.isEmpty()) {
                iter.remove();
            }
        }

        results.add(aFds);
        results.add(bFds);
        return results;
    }

    public List<Map<Set<String>, Set<String>>> toBCNF(Set<Set<String>> candidateKeys) {
        List<Map<Set<String>, Set<String>>> results = new ArrayList<Map<Set<String>, Set<String>>>();
        Set<Set<String>> superKeys = findSuperKeys(candidateKeys);
        Set<String> A = new HashSet<String>();
        Set<String> B = new HashSet<String>(attributes);
        Map<Set<String>, Set<String>> aFds = new LinkedHashMap<>();
        Map<Set<String>, Set<String>> bFds = new LinkedHashMap<>();

        for (Entry<Set<String>, Set<String>> fd : fds.entrySet()) {
            if (!superKeys.contains(fd.getKey())) {
                A.addAll(fd.getKey());
                A.addAll(fd.getValue());
                Set<String> temp = new HashSet<String>(B);
                temp.removeAll(A);
                B.removeAll(temp);
                break;
            }
        }

        for (Entry<Set<String>, Set<String>> fd : fds.entrySet()) {
            Set<String> attributesInFd = getAttribsInFD(fd);

            if (A.containsAll(attributesInFd)) {
                aFds.put(fd.getKey(), fd.getValue());
            }
            if (B.containsAll(attributesInFd)) {
                bFds.put(fd.getKey(), fd.getValue());
            }
        }

        results.add(aFds);
        results.add(bFds);
        return results;

    }

    private Set<String> getAttribsInFD(Entry<Set<String>, Set<String>> fd) {
        Set<String> attributesInFd = new HashSet<String>();
        for (String string : fd.getKey()) {
            if (!attributesInFd.contains(string)) {
                attributesInFd.add(string);
            }
        }
        for (String string : fd.getValue()) {
            if (!attributesInFd.contains(string)) {
                attributesInFd.add(string);
            }
        }

        return attributesInFd;
    }

    public Set<String> getAttribsInFDs(Map<Set<String>, Set<String>> fds) {
        Set<String> attributesInFds = new HashSet<String>();
        for (Entry<Set<String>, Set<String>> fd : fds.entrySet()) {
            Set<String> attributesInFd = getAttribsInFD(fd);
            attributesInFds.addAll(attributesInFd);
        }

        return attributesInFds;
    }

    public String sortString(String inputString) {
        // convert input string to char array
        char tempArray[] = inputString.toCharArray();

        // sort tempArray
        Arrays.sort(tempArray);

        // return new sorted string
        return new String(tempArray);
    }
}