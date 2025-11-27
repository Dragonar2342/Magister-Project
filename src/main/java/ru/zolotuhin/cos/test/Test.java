package ru.zolotuhin.cos.test;

import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) {
//        String sentence = "The quick brown fox jumps over the lazy dog.";
//        System.out.println(check(sentence));
//        int n = 9119;
//        System.out.println(squareDigits(n));
//        int[] a = new int[] {1,2,3,4,3,2,1};
//        System.out.println(findEvenIndex(a));
//        List<Integer> list = new ArrayList<>(Arrays.asList(0,1,0,1));
//        System.out.println(ConvertBinaryArrayToInt(list));
//        int n = 695, p = 2;
//        System.out.println(digPow(n, p));
//        System.out.println(encode("Prespecialized"));
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        for(int i : list){
            list.remove(1);
        }

        System.out.println(list);
    }

    public static boolean check(String sentence){
        int count = 0;
        for (int i = 0; i <sentence.length(); i++) {
            char c = sentence.charAt(i);
            for (int j = 0; j <sentence.length(); j++) {
                if ((i == j) || (c == 32)) {
                    break;
                }
                System.out.println(c + " = " + sentence.charAt(j));
            }
            count++;
        }
        if (count == sentence.length()) {}
        return true;
    }

    public static int squareDigits(int n) {
        String n_str = Integer.toString(n);
        StringBuilder finalNumber = new StringBuilder();
        for (int i = 0; i < n_str.length(); i++) {
            finalNumber.append(Integer.parseInt(String.valueOf(n_str.charAt(i)))*Integer.parseInt(String.valueOf(n_str.charAt(i))));
        }
        return Integer.parseInt(finalNumber.toString());

    }

    public static int findEvenIndex(int[] arr) {
        int index = -1;
        for (int i = 1; i < arr.length; i++) {
            int sum1 = 0, sum2 = 0;
            for (int j = arr.length - 1; j > i; j--) {
                System.out.print(arr[j] + " ");
                sum1 += arr[j];
            }
            for (int z = 0; z < i; z++) {
                System.out.print(arr[z] + " ");
                sum2 += arr[z];
            }

            if (sum1 == sum2) {
                index = i;
            }
        }
        return index;
    }

    public static int ConvertBinaryArrayToInt(List<Integer> binary) {
        int number = 0;
        List<Integer> value = new ArrayList<>();
        boolean start = false;
        for (Integer integer : binary) {
            if (integer == 1) {
                start = true;
            }
            if (start) {
                System.out.print(integer + " ");
                value.add(integer);
            }
        }
        System.out.println();
        System.out.println(value);
        int degree = 0;
        for (int i = value.size() - 1; i >= 0; i--) {
            number += (int) (value.get(i) * Math.pow(2, degree++));
        }
        return number;
    }

    public static long digPow(int n, int p) {
        int sum = 0;
        int digit = n;
        int len = String.valueOf(n).length();
        for (int i = len-1; i >= 0; i--) {
            int a = (int) (n / Math.pow(10,i));
            sum += (int) Math.pow(a, p++);
            n %= (int) Math.pow(10,i);
        }
        if ((sum / digit) % 1 == 0 && (sum / digit) > 0) {

            return sum / digit;
        }
        return -1;
    }

    static String encode(String word){
        word = word.toLowerCase();
        StringBuilder new_word = new StringBuilder();
        boolean encount = false;
        for (int i = 0; i < word.length(); i++) {
            for (int j = 0; j < word.length(); j++) {
                if (word.charAt(i) == word.charAt(j) && j !=i) {
                    encount = true;
                    break;
                } else {
                    encount = false;
                }
            }
            if (encount) {
                System.out.println(word.charAt(i) + " " + ")");
                new_word.append(")");
            } else {
                System.out.println(word.charAt(i) + " " + "(");
                new_word.append("(");
            }

        }
        return new_word.toString();
    }
}
