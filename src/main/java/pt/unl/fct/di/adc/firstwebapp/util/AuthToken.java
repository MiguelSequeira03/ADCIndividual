package pt.unl.fct.di.adc.firstwebapp.util;

import java.util.UUID;

public class AuthToken {
	
	
	//TODO ISSUEDAT DEVE SER INT OU LONG??
	public static final long EXPIRATION_TIME = 1000*60*60*2; //2h
		public String tokenId;
		public String username;
		public String role;
		public long issuedAt;
		public long expiredAt;
		
		public AuthToken() {
			
		}
		public AuthToken(String username, String role) {
			this.tokenId = UUID.randomUUID().toString();
			this.username = username;
			this.role = role;
			this.issuedAt = System.currentTimeMillis();
			this.expiredAt = this.issuedAt + AuthToken.EXPIRATION_TIME;
		}
		public boolean isValid() {
			return this.username != null && 
					   this.role != null && this.tokenId != null
					   && issuedAt != 0 && expiredAt != 0;
		
		}
}
