package com.mealspire.app.domain;
import java.io.IOException;
public interface DishImporter { CookbookEntry importDish(String input) throws IOException; }
