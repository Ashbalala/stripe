package edu.cit.taskbounty;

import edu.cit.taskbounty.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

class TaskbountyApplicationTests {


	public static void main(String[] args) {
		JwtUtil jwtUtil = new JwtUtil();
		String token = jwtUtil.generateJwtToken("testuser");
		System.out.println("Token: " + token);
		System.out.println("Parsed user: " + jwtUtil.getUserNameFromJwtToken("Bearer " + token));

	}

}
