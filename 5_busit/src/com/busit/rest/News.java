package com.busit.rest;

import com.anotherservice.db.*;
import com.anotherservice.rest.*;
import com.anotherservice.rest.security.*;
import com.anotherservice.util.*;
import com.busit.security.*;
import java.util.*;

public class News extends InitializerOnce
{
	public void initialize()
	{
		Index index = new Index();
		index.addMapping(new String[] { "news" });
		index.description = "Manages news";
		Handler.addHandler("/busit/", index);
		
		initializeInsert(index);
		initializeUpdate(index);
		initializeDelete(index);
		initializeSelect(index);
	}
	
	private void initializeInsert(Index index)
	{
		Action insert = new Action()
		{
			public Object execute() throws Exception
			{
				String title = getParameter("title").getValue();
				String description = getParameter("description").getValue();
				String content = getParameter("content").getValue();
				String author = getParameter("author").getValue();
				String authorname = getParameter("authorname").getValue();
				String language = getParameter("language").getValue();
				
				if( title.matches("^[0-9]+$") || title.matches("(^[\\._\\-\\s]|[\\._\\-\\s]$)") )
					throw new Exception("The news title may not be numeric and may not start or end with special characters");
				
				Long uid = Database.getInstance().insert("INSERT INTO news (news_title, news_description, news_content, news_author, news_author_name, news_date, news_language) VALUES " + 
					"('" + Security.escape(title) + "', '" + Security.escape(description) + "', '" + Security.escape(content) + "', '" + Security.escape(author) + "', '" + Security.escape(authorname) + "', UNIX_TIMESTAMP(), '" + Security.escape(language) + "')");
				
				Hashtable<String, Object> result = new Hashtable<String, Object>();
				result.put("title", title);
				result.put("id", uid);
				return result;
			}
		};
		
		insert.addMapping(new String[] { "insert", "create", "add" });
		insert.description = "Post a news";
		insert.returnDescription = "The created news {'title', 'id'}";
		insert.addGrant(new String[] { "access", "news_insert" });
		
		Parameter title = new Parameter();
		title.isOptional = false;
		title.minLength = 3;
		title.maxLength = 200;
		title.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		title.description = "The news title";
		title.addAlias(new String[]{ "title", "news_title" });
		insert.addParameter(title);

		Parameter description = new Parameter();
		description.isOptional = false;
		description.minLength = 3;
		description.maxLength = 2000;
		description.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		description.description = "The news description";
		description.addAlias(new String[]{ "description", "news_description" });
		insert.addParameter(description);
		
		Parameter content = new Parameter();
		content.isOptional = false;
		content.minLength = 3;
		content.maxLength = 10000;
		content.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		content.description = "The news content";
		content.addAlias(new String[]{ "content", "news_content" });
		insert.addParameter(content);

		Parameter author = new Parameter();
		author.isOptional = false;
		author.minLength = 3;
		author.maxLength = 200;
		author.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		author.description = "The news author";
		author.addAlias(new String[]{ "author", "news_author" });
		insert.addParameter(author);

		Parameter authorname = new Parameter();
		authorname.isOptional = true;
		authorname.minLength = 2;
		authorname.maxLength = 200;
		authorname.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		authorname.description = "The news author name";
		authorname.addAlias(new String[]{ "authorname", "author_name" });
		insert.addParameter(authorname);
		
		Parameter language = new Parameter();
		language.isOptional = false;
		language.minLength = 1;
		language.maxLength = 2;
		language.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		language.description = "The news language";
		language.addAlias(new String[]{ "language", "news_language" });
		insert.addParameter(language);
		
		index.addOwnHandler(insert);
	}

	
	private void initializeUpdate(Index index)
	{
		Action update = new Action()
		{
			public Object execute() throws Exception
			{
				String id = getParameter("id").getValue();
				String title = getParameter("title").getValue();
				String description = getParameter("description").getValue();
				String content = getParameter("content").getValue();
				String author = getParameter("author").getValue();
				String authorname = getParameter("authorname").getValue();
				String language = getParameter("language").getValue();
				String status = getParameter("status").getValue();
				
				String set = "";
				if( title != null )
					set += "news_title = '" + Security.escape(title) + "', ";
				if( description != null )
					set += "news_description = '" + Security.escape(description) + "', ";
				if( content != null )
					set += "news_content = '" + Security.escape(content) + "', ";
				if( author != null )
					set += "news_author = '" + Security.escape(author) + "', ";
				if( authorname != null )
					set += "news_author_name = '" + Security.escape(authorname) + "', ";
				if( language != null )
					set += "news_language = '" + Security.escape(language) + "', ";
				if( status != null )
					set += "news_status = '" + Security.escape(status) + "', ";
					
				Database.getInstance().update("UPDATE news SET " + set + " news_id = " + Security.escape(id) + " WHERE news_id = " + Security.escape(id));
				
				return "OK";
			}
		};
		
		update.addMapping(new String[] { "update", "modify", "change" });
		update.description = "Update a news";
		update.returnDescription = "OK";
		update.addGrant(new String[] { "access", "news_update" });

		Parameter id = new Parameter();
		id.isOptional = false;
		id.minLength = 1;
		id.maxLength = 11;
		id.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		id.description = "The news id";
		id.addAlias(new String[]{ "id", "news_id" });
		update.addParameter(id);
		
		Parameter title = new Parameter();
		title.isOptional = true;
		title.minLength = 3;
		title.maxLength = 200;
		title.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		title.description = "The news title";
		title.addAlias(new String[]{ "title", "news_title" });
		update.addParameter(title);

		Parameter description = new Parameter();
		description.isOptional = true;
		description.minLength = 3;
		description.maxLength = 2000;
		description.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		description.description = "The news description";
		description.addAlias(new String[]{ "description", "news_description" });
		update.addParameter(description);
		
		Parameter content = new Parameter();
		content.isOptional = true;
		content.minLength = 3;
		content.maxLength = 10000;
		content.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		content.description = "The news content";
		content.addAlias(new String[]{ "content", "news_content" });
		update.addParameter(content);

		Parameter author = new Parameter();
		author.isOptional = true;
		author.minLength = 3;
		author.maxLength = 200;
		author.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		author.description = "The news author";
		author.addAlias(new String[]{ "author", "news_author" });
		update.addParameter(author);

		Parameter authorname = new Parameter();
		authorname.isOptional = true;
		authorname.minLength = 2;
		authorname.maxLength = 200;
		authorname.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALL);
		authorname.description = "The news author name";
		authorname.addAlias(new String[]{ "authorname", "author_name" });
		update.addParameter(authorname);
		
		Parameter language = new Parameter();
		language.isOptional = true;
		language.minLength = 1;
		language.maxLength = 2;
		language.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		language.description = "The news language";
		language.addAlias(new String[]{ "language", "news_language" });
		update.addParameter(language);
	
		Parameter status = new Parameter();
		status.isOptional = true;
		status.minLength = 1;
		status.maxLength = 1;
		status.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		status.description = "Is this news published (1|0)?";
		status.addAlias(new String[]{ "status", "news_status" });
		update.addParameter(status);
		
		index.addOwnHandler(update);
	}
	
	private void initializeDelete(Index index)
	{
		Action delete = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> news = getParameter("news").getValues();
				
				String where = "1>1";
				for( String n : news )
				{
					if( !n.matches("^[0-9]+$") )
						where += " OR news_title = '" + Security.escape(n) + "'";
					else
						where += " OR news_id = " + n;
						
				}
				
				Database.getInstance().delete("DELETE FROM news WHERE  " + where);
				return "OK";
			}
		};
		
		delete.addMapping(new String[] { "delete", "del", "remove", "destroy" });
		delete.description = "Removes a news";
		delete.returnDescription = "OK";
		delete.addGrant(new String[] { "access", "news_delete" });
		
		Parameter news = new Parameter();
		news.isOptional = false;
		news.minLength = 1;
		news.maxLength = 200;
		news.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		news.allowInUrl = true;
		news.isMultipleValues = true;
		news.description = "The news title(s) or id(s)";
		news.addAlias(new String[]{ "news", "news_title", "id", "news", "nid", "titles", "ids", "nids" });
		delete.addParameter(news);
				
		index.addOwnHandler(delete);
	}
	
	private void initializeSelect(Index index)
	{
		Action select = new Action()
		{
			public Object execute() throws Exception
			{
				Collection<String> news = getParameter("news").getValues();
				String language = getParameter("language").getValue();
				String category = getParameter("category").getValue();
				String status = getParameter("status").getValue();
				
				String where = "";
				if( news.size() > 0 )
				{
					where += " AND (1>1";
					for( String n : news )
					{
						if( n.matches("^[0-9]+$") )
							where += " OR news_id = " + n;
						else
							where += " OR news_title LIKE '%" + Security.escape(n) + "%'";
					}
					where += ")";
				}
				if( language != null )
					where += " AND news_language = '" + Security.escape(language) + "'";
				if( category != null )
					where += " AND news_category = '" + Security.escape(category) + "'";
				if( status != null )
					where += " AND news_status = '" + Security.escape(status) + "'";
					
				return Database.getInstance().select("SELECT news_id, news_title, news_description, news_content, news_author, news_author_name, news_date, news_language, news_status, news_category FROM news WHERE 1=1 " + where + " ORDER BY news_date DESC");
			}
		};
		
		select.addMapping(new String[] { "select", "list", "search", "find", "view" });
		select.description = "Retrieves information about a news. Multiple search values for a single property will be OR'd and different criteria will be AND'd.";
		select.returnDescription = "The matching news [{'title', 'id', 'description', 'content', 'author', 'date'},...]";
		select.addGrant(new String[] { "access", "news_select" });
		
		Parameter news = new Parameter();
		news.isOptional = true;
		news.minLength = 1;
		news.maxLength = 200;
		news.mustMatch = PatternBuilder.getRegex(PatternBuilder.ALPHANUM | PatternBuilder.PUNCT | PatternBuilder.SPACE);
		news.allowInUrl = true;
		news.isMultipleValues = true;
		news.description = "The news title(s) or id(s)";
		news.addAlias(new String[]{ "news", "news_title", "id", "news", "nid", "titles", "ids", "nids" });
		select.addParameter(news);
	
		Parameter language = new Parameter();
		language.isOptional = true;
		language.minLength = 1;
		language.maxLength = 2;
		language.mustMatch = PatternBuilder.getRegex(PatternBuilder.UPPER);
		language.description = "The news language";
		language.addAlias(new String[]{ "language", "news_language" });
		select.addParameter(language);
	
		Parameter category = new Parameter();
		category.isOptional = true;
		category.minLength = 1;
		category.maxLength = 2;
		category.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		category.description = "The news category";
		category.addAlias(new String[]{ "category", "news_category" });
		select.addParameter(category);

		Parameter status = new Parameter();
		status.isOptional = true;
		status.minLength = 1;
		status.maxLength = 1;
		status.mustMatch = PatternBuilder.getRegex(PatternBuilder.NUMBER);
		status.description = "Select published or unpublished";
		status.addAlias(new String[]{ "status", "news_status" });
		select.addParameter(status);
		
		index.addOwnHandler(select);
	}
}
