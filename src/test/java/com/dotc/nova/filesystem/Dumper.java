package com.dotc.nova.filesystem;

import com.dotc.nova.Nova;

public class Dumper {
	public static void main(String[] args) throws Exception {
		int size = 600000;
		char[] ca = new char[size];

		for (int i = 0; i < size; i++) {
			ca[i] = (char) (97 + i % 26);
		}

		Nova nova = new Nova.Builder().build();
		Filesystem f = nova.filesystem;

		String content = new String(ca);
		f.writeFileSync(content, "bla.txt", true);

		for (int i = size; i > 0; i -= 25) {
			Thread.sleep(3000);
			content = content.substring(0, content.length() - 25);
			System.out.println("     writing " + i + " chars... (Ends with" + content.substring(content.length() - 20) + ")");
			f.writeFileSync(content, "bla.txt", false);
		}

		System.out.println("Done!");
	}
}
