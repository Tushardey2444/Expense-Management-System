package com.manage_expense.config;

import com.manage_expense.entities.Category;
import com.manage_expense.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class CategorySeeder implements ApplicationRunner {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {

        if (categoryRepository.count() > 0) {
            return;
        }

        createCategory("Monthly",
                "Groceries",
                "Restaurant",
                "Coffee & Snacks",
                "Food",
                "Others"
        );

        createCategory("Marriage",
                "Food",
                "Restaurant"
                );

        createCategory("Travel",
                "Food",
                "Electronics",
                "Taxi / Ride Share"
        );

        createCategory("Shopping",
                "Clothing",
                "Electronics",
                "Online Shopping"
        );

        createCategory("Health",
                "Pharmacy",
                "Doctor / Medical",
                "Fitness / Gym"
        );

        createCategory("Entertainment",
                "Movies",
                "Games",
                "Streaming Services"
        );

        createCategory("Bills & Utilities",
                "Electricity",
                "Internet",
                "Water"
        );

        createCategory("Travel",
                "Flights",
                "Hotels",
                "Vacation"
        );

        createCategory("Others",
                "Miscellaneous"
        );
    }

    private void createCategory(String parentName, String... subNames) {

        Category parent = Category.builder()
                .categoryName(parentName)
                .isDefault(true)
                .description("Default parent category for " + parentName)
                .build();

        categoryRepository.save(parent);

        List<Category> existingSubCategories = new ArrayList<>();
        for (String subName : subNames) {
            Category child = Category.builder()
                    .categoryName(subName)
                    .parentCategory(parent)
                    .isDefault(true)
                    .description("Default subcategory for " + parentName)
                    .build();
            existingSubCategories.add(child);
        }
        categoryRepository.saveAll(existingSubCategories);
    }
}
