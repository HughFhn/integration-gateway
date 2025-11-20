// **File used for logging alterations to REDcap (Could be used as way to know when to pull data, if record is altered or added). Uncomment this file and alter application.properties to include a url and api token and should function**

// package com.example.gateway.REDCap;

// import java.io.BufferedReader;
// import java.io.InputStreamReader;
// import java.text.SimpleDateFormat;
// import java.util.ArrayList;
// import java.util.Date;
// import java.util.List;

// //import com.example.gateway.routes.REDCapRoute;
// import org.apache.http.HttpResponse;
// import org.apache.http.NameValuePair;
// import org.apache.http.client.entity.UrlEncodedFormEntity;
// import org.apache.http.client.methods.HttpPost;
// import org.apache.http.impl.client.HttpClientBuilder;
// import org.apache.http.message.BasicNameValuePair;
// //import static com.example.gateway.routes.REDCapRoute.*;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;
// import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
// import org.apache.http.impl.client.CloseableHttpClient;
// import javax.net.ssl.SSLContext;
// import java.io.FileInputStream;
// import java.io.FileNotFoundException;
// import java.io.IOException;
// import java.security.KeyStore;

// @Service

// public class REDCapLogs {

//     private static final Logger log = LoggerFactory.getLogger(REDCapLogs.class);

// private final REDCapConfig redCapConfig;
//      @Autowired
//      public REDCapLogs(REDCapConfig redCapConfig) {
//          this.redCapConfig = redCapConfig;
//      }
//     public String getLogs() {
//         try {
//             String beginTime = "2025-10-21 12:41";
//             String endTime = getEndTime();

//             List<NameValuePair> params = new ArrayList<NameValuePair>();
//             params.add(new BasicNameValuePair("token", redCapConfig.getApiToken()));
//             params.add(new BasicNameValuePair("content", "log"));
//             params.add(new BasicNameValuePair("logtype", "record"));
//             params.add(new BasicNameValuePair("user", ""));
//             params.add(new BasicNameValuePair("record", ""));
//             params.add(new BasicNameValuePair("beginTime", beginTime)); // Logged after
//             params.add(new BasicNameValuePair("endTime", endTime)); // Logged before
//             params.add(new BasicNameValuePair("format", "json"));
//             params.add(new BasicNameValuePair("returnFormat", "json"));

//             KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
//             String trustStorePath = "src/main/resources/ssl/redcap-truststore.jks";
//             log.info("Truststore path: {}", trustStorePath);

//             try (FileInputStream fis = new FileInputStream(trustStorePath)) {
//                 trustStore.load(fis, "changeit".toCharArray());
//                 log.info("Truststore loaded successfully.");
//             } catch (FileNotFoundException e) {
//                 log.error("Truststore file not found at {}. Please ensure the file exists.", trustStorePath, e);
//                 return null;
//             } catch (IOException e) {
//                 log.error("Error loading truststore from {}. Please check the file and password.", trustStorePath, e);
//                 return null;
//             }

//             SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
//                     .loadTrustMaterial(trustStore, null)
//                     .build();

//             SSLConnectionSocketFactory sslFactory = new SSLConnectionSocketFactory(sslContext);

//             CloseableHttpClient client = HttpClientBuilder.create()
//                     .setSSLSocketFactory(sslFactory)
//                     .build();

//             HttpPost post = new HttpPost(redCapConfig.getApiUrl());
//             post.setHeader("Content-Type", "application/x-www-form-urlencoded");
//             post.setEntity(new UrlEncodedFormEntity(params));

//             HttpResponse response = client.execute(post);
//             BufferedReader reader = new BufferedReader(
//                     new InputStreamReader(response.getEntity().getContent())
//             );

//             StringBuilder result = new StringBuilder();
//             String line;
//             while ((line = reader.readLine()) != null) {
//                 result.append(line);
//             }

//             log.info("Retrieved Logs from REDCap");
//             System.out.println(result);
//             return result.toString();

//         } catch (Exception e) {
//             log.error("Error retrieving logs", e);
//             return null;
//         }
//     }

//     public String getEndTime() {
//         Date currentTime = new Date();
//         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
//         String endTime = sdf.format(currentTime);
//         return endTime;
//     }

// }
