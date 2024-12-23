import okhttp3.*;
import com.google.gson.*;
import java.io.IOException;

public class PaystackPaymentProcessor {

    private static final String BASE_URL = "https://api.paystack.co";
    private static final String API_KEY = "Bearer sk_test_6b7b56370c6055142cf2e212e33c5dd4ad200ef7";
    private final OkHttpClient client;
    private final Gson gson;

    public PaystackPaymentProcessor() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    public String deposit(String email, int amount) throws IOException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        if (!isValid(email)) {
            throw new IllegalArgumentException("Email Address is not valid");
        }

        String url = BASE_URL + "/transaction/initialize";

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("email", email);
        bodyJson.addProperty("amount", amount);

        RequestBody body = RequestBody.create(bodyJson.toString(), MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", API_KEY)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }

    public String withdraw(String accountNumber, String bankCode, int amount) throws IOException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        String url = BASE_URL + "/transfer";

        JsonObject recipientBody = new JsonObject();
        recipientBody.addProperty("type", "nuban");
        recipientBody.addProperty("name", "Recipient Name");
        recipientBody.addProperty("account_number", accountNumber);
        recipientBody.addProperty("bank_code", bankCode);
        
        RequestBody recipientRequestBody = RequestBody.create(recipientBody.toString(), MediaType.get("application/json"));

        Request recipientRequest = new Request.Builder()
                .url(BASE_URL + "/transferrecipient")
                .addHeader("Authorization", API_KEY)
                .post(recipientRequestBody)
                .build();

        String recipientCode;
        try (Response recipientResponse = client.newCall(recipientRequest).execute()) {
            if (!recipientResponse.isSuccessful()) {
                throw new IOException("Failed to create transfer recipient: " + recipientResponse);
            }

            JsonObject recipientResponseJson = gson.fromJson(recipientResponse.body().string(), JsonObject.class);
            recipientCode = recipientResponseJson.getAsJsonObject("data").get("recipient_code").getAsString();
        }

        JsonObject transferBody = new JsonObject();
        transferBody.addProperty("source", "balance");
        transferBody.addProperty("amount", amount);
        transferBody.addProperty("recipient", recipientCode);

        RequestBody transferRequestBody = RequestBody.create(transferBody.toString(), MediaType.get("application/json"));

        Request transferRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", API_KEY)
                .post(transferRequestBody)
                .build();

        try (Response response = client.newCall(transferRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }

    public static boolean isValid(String email) {
      
        // Regular expression to match valid email formats
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                            "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    
        // Compile the regex
        Pattern p = Pattern.compile(emailRegex);
      
        // Check if email matches the pattern
        return email != null && p.matcher(email).matches();
    }

    public String chargeCard(String email, int amount, JsonObject cardDetails) throws IOException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        String url = BASE_URL + "/charge";

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("email", email);
        bodyJson.addProperty("amount", amount);
        bodyJson.add("card", cardDetails);

        RequestBody body = RequestBody.create(bodyJson.toString(), MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", API_KEY)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }

    public String submitOtp(String reference, String otp) throws IOException {
        String url = BASE_URL + "/charge/submit_otp";

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("reference", reference);
        bodyJson.addProperty("otp", otp);

        RequestBody body = RequestBody.create(bodyJson.toString(), MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", API_KEY)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            return response.body().string();
        }
    }

    public static void main(String[] args) {
        PaystackPaymentProcessor processor = new PaystackPaymentProcessor();

        try {
            //  deposit
            String depositResponse = processor.deposit("customer@paystack.com", 50000);
            System.out.println("Deposit Response: " + depositResponse);

            //  withdrawal
            String withdrawalResponse = processor.withdraw("1234567890", "058", 100000);
            System.out.println("Withdrawal Response: " + withdrawalResponse);


            //  card details
            JsonObject cardDetails = new JsonObject();
            cardDetails.addProperty("number", "4084084084084081");
            cardDetails.addProperty("cvv", "408");
            cardDetails.addProperty("expiry_month", "12");
            cardDetails.addProperty("expiry_year", "31");

            //  charge
            String chargeResponse = processor.chargeCard("customer@paystack.com", 50000, cardDetails);
            System.out.println("Charge Response: " + chargeResponse);

            //  OTP submission
            String otpResponse = processor.submitOtp("transaction_reference", "123456");
            System.out.println("OTP Response: " + otpResponse);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
