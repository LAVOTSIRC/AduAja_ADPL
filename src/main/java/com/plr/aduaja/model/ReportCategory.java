package com.plr.aduaja.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;

@Entity
@Table(name = "report_categories")
public class ReportCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "category_id")
    private String categoryId;

    @Column(nullable = false, unique = true)
    private String categoryName;

    @Column(nullable = false)
    private Integer slaDurationHours = 72;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String iconName;

    @Column(nullable = false)
    private Boolean isActive = true;

    public ReportCategory() {}

    public ReportCategory(String categoryName, Integer slaDurationHours, String description, String iconName) {
        this.categoryName = categoryName;
        this.slaDurationHours = slaDurationHours;
        this.description = description;
        this.iconName = iconName;
    }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getName() { return categoryName; }
    public void setName(String name) { this.categoryName = name; }

    public Integer getSlaDurationHours() { return slaDurationHours; }
    public void setSlaDurationHours(Integer slaDurationHours) { this.slaDurationHours = slaDurationHours; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
