package pt.unl.fct.di.adc.firstwebapp.util;

public class RegisterData {
    public String username;
    public String password;
    public String confirmation;
    public String phone;
    public String address;
    public String role;

    public RegisterData() {
    }

    public RegisterData(String username, String password, String address, String confirmation, String phone, String role) {
        this.username = username;
        this.password = password;
        this.address = address;
        this.confirmation = confirmation;
        this.phone = phone;
        this.role = role;
    }

	public boolean validRegistration() {
			return this.username != null && this.password != null && 
				   this.address != null && this.confirmation != null && this.phone != null && this.role != null
				   && this.password.equals(this.confirmation);
	
	}

}
