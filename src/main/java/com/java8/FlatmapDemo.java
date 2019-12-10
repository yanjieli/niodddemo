package com.java8;

import java.util.Arrays;
import java.util.stream.Stream;

public class FlatmapDemo {

	public static void main(String[] args) {
		String data[][] = new String[][] { { "1", "2" }, { "3", "4" } };
		Stream<String[]> temp = Arrays.stream(data);
//		temp.filter(x -> x.length > 0);
		Stream<String> stringStream = temp.flatMap(x -> Arrays.stream(x));
		stringStream.filter(x -> Integer.parseInt(x) > 2).forEach(System.out::println);

	}

}
