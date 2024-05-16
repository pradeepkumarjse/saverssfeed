import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GetCompanyList {

    public static void main(String[] args) {
        // JDBC connection parameters
        String url = "jdbc:mysql://localhost:3306/company_data";
        String username = "root";
        String password = "root";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // Connect to the database
            conn = DriverManager.getConnection(url, username, password);

            // Fetch XML response from the URL
            String xmlResponse = fetchXMLResponse("https://www.sec.gov/cgi-bin/browse-edgar?action=getcurrent&CIK=&type=&company=&dateb=&owner=include&start=0&count=40&output=atom");

            System.out.println(xmlResponse);

            // Parse XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlResponse)));



            // Retrieve entry titles
            NodeList entryList = doc.getElementsByTagName("entry");
            for (int i = 0; i < entryList.getLength(); i++) {
                Element entry = (Element) entryList.item(i);
                String title = entry.getElementsByTagName("title").item(0).getTextContent();
                System.out.println(title);
                // Check if the title already exists in the database
                String query = "SELECT COUNT(*) FROM company_name WHERE name = ?";
                pstmt = conn.prepareStatement(query);
                pstmt.setString(1, title);
                rs = pstmt.executeQuery();
                rs.next();
                int count = rs.getInt(1);

                if (count == 0) { // If the title doesn't exist, insert it into the database
                    query = "INSERT INTO company_name (name) VALUES (?)";
                    pstmt = conn.prepareStatement(query);
                    pstmt.setString(1, title);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException | ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static String fetchXMLResponse(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
      connection.setRequestProperty("User-Agent", "PostmanRuntime/7.37.3");

        InputStream inputStream = connection.getInputStream();
        StringBuilder response = new StringBuilder();
        int bytesRead;
        byte[] buffer = new byte[1024];
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            response.append(new String(buffer, 0, bytesRead));
        }
        inputStream.close();

        return response.toString();
    }
}
