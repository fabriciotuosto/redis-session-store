package org.redis.session.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;

import org.apache.catalina.Session;
import org.apache.catalina.Store;
import org.apache.catalina.session.StoreBase;

import redis.clients.jedis.Jedis;
import java.util.logging.Logger;

public class RedisSessionStore extends StoreBase implements Store{

	private static final Logger LOGGER = Logger.getLogger("RedisSessionStore");
	
	private volatile Jedis jedis;
	private String host;
	private int port;
	private int database;
	private String password;
	/**
	 * @throws IOException
	 */
	public RedisSessionStore() throws IOException {

	}
	
	@Override
	public void clear() throws IOException {
		LOGGER.info("Clearing Session database");
		getJedis().flushDB();
	}

	@Override
	public int getSize() throws IOException {
		LOGGER.info("Returning size of Session database");
		return getJedis().dbSize().intValue();
	}

	@Override
	public String[] keys() throws IOException {
		String[] keys = null;
		Set<String> keysStored = getJedis().keys("*");
		keys = new String[keysStored.size()];
		keysStored.toArray(keys);
		LOGGER.info("Retrieving keys: "+Arrays.asList(keys));
		return keys;
	}

	@Override
	public Session load(String key) throws ClassNotFoundException, IOException {
		Session session = null;
		if(isNotEmpty(key)){
			session = fromByteArray(getJedis().get(key.getBytes()));
		}
		session = session != null ? session : createEmptySession();
		LOGGER.info("Loading key: "+key+" retrieving session: "+session);
		return session;
	}

	private boolean isNotEmpty(String key) {
		return key != null && "".equals(key.trim());
	}

	private Session createEmptySession() throws IOException {
		Session session = getManager().createEmptySession();
		return session;
	}

	@Override
	public void remove(String key) throws IOException {
		LOGGER.info("Removing session with key :"+String.valueOf(key));
		if(isNotEmpty(key)){
			getJedis().del(key.getBytes());
		}
	}

	@Override
	public void save(Session session) throws IOException {
		LOGGER.info("Saving session :"+session);
		if(session != null)
			getJedis().set(session.getId().getBytes(), toByteArray(session));
		
	}

	private Jedis getJedis() throws UnknownHostException, IOException{
		Jedis local = jedis;
		if(local == null || !local.isConnected()){
			synchronized (this) {
				local = jedis;
				if(local == null || !local.isConnected()){
					jedis = local = createClient();
				}
			}
		}
		return local;
	}
	
	private Jedis createClient() throws UnknownHostException, IOException {
		LOGGER.info("Creating new jedis client");
		Jedis client = new Jedis(host, port);
		client.select(database);
		if(isNotEmpty(password)){
			client.auth(password);
		}
		client.connect();
		return client;
	}

	private byte[] toByteArray(Session session) throws IOException {
		byte[] result = null;
		ByteArrayOutputStream output = null;
		ObjectOutputStream objectOutput = null;
		try{
			output = new ByteArrayOutputStream();
			objectOutput = new ObjectOutputStream(output);
			objectOutput.writeObject(session);
			result = output.toByteArray();
		}finally{
			if(output != null)
				output.close();
			if(objectOutput != null)
				objectOutput.close();
		}
		return result;
	}
	
	/**
	 * @param bytes
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private Session fromByteArray(byte[] bytes) throws IOException, ClassNotFoundException {
		Session session = null;
		ByteArrayInputStream stream = null;
		ObjectInputStream objectStream = null;
		try{
			stream = new ByteArrayInputStream(bytes);
			objectStream = new ObjectInputStream(stream);
			session = (Session) objectStream.readObject();
		}finally{
			if(stream != null)
				stream.close();
			if(objectStream != null)
				objectStream.close();
		}
		return session;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getDatabase() {
		return database;
	}

	public void setDatabase(int database) {
		this.database = database;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
