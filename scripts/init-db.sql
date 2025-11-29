-- Create databases for each service
CREATE DATABASE productdb;
CREATE DATABASE orderdb;
CREATE DATABASE inventorydb;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE productdb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE orderdb TO postgres;
GRANT ALL PRIVILEGES ON DATABASE inventorydb TO postgres;
