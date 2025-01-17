package net.mitask.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Map of status code values and meanings.
 * @author MiTask
 * @since 1.0.0
 */
public class HttpStatusCode {
    public static final Map<Integer, String> STATUS_CODES = new HashMap<>() {{
            put(100, "Continue");
            put(101, "Switching Protocols");
            put(102, "Processing");
            put(103, "Early Hints");

            put(200, "OK");
            put(201, "Created");
            put(202, "Accepted");
            put(203, "Non-Authoritative Information");
            put(204, "No Content");
            put(205, "Reset Content");
            put(206, "Partial Content");
            put(207, "Multi-Status");
            put(208, "Already Reported");
            put(226, "IM Used");

            put(300, "Multiple Choices");
            put(301, "Moved Permanently");
            put(302, "Found");
            put(303, "See Other");
            put(304, "Not Modified");
            put(307, "Temporary Redirect");
            put(308, "Permanent Redirect");

            put(400, "Bad Request");
            put(401, "Unauthorized");
            put(402, "Payment Required");
            put(403, "Forbidden");
            put(404, "Not Found");
            put(405, "Method Not Allowed");
            put(406, "Not Acceptable");
            put(407, "Proxy Authentication Required");
            put(408, "Request Timeout");
            put(409, "Conflict");
            put(410, "Gone");
            put(411, "Length Required");
            put(412, "Precondition Failed");
            put(413, "Payload Too Large");
            put(414, "URI Too Long");
            put(415, "Unsupported Media Type");
            put(416, "Range Not Satisfiable");
            put(417, "Expectation Failed");
            put(418, "I'm a teapot");
            put(421, "Misdirected Request");
            put(422, "Unprocessable Entity");
            put(423, "Locked");
            put(424, "Failed Dependency");
            put(425, "Too Early");
            put(426, "Upgrade Required");
            put(428, "Precondition Required");
            put(429, "Too Many Requests");
            put(431, "Request Header Fields Too Large");
            put(451, "Unavailable For Legal Reasons");

            put(500, "Internal Server Error");
            put(501, "Not Implemented");
            put(502, "Bad Gateway");
            put(503, "Service Unavailable");
            put(504, "Gateway Timeout");
            put(505, "HTTP Version Not Supported");
            put(506, "Variant Also Negotiates");
            put(507, "Insufficient Storage");
            put(508, "Loop Detected");
            put(510, "Not Extended");
            put(511, "Network Authentication Required");
    }};
}