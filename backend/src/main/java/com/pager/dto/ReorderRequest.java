package com.pager.dto;

import java.util.List;

public class ReorderRequest {
    public List<Item> items;

    public static class Item {
        public Long id;
        public Integer sortOrder;
        public Double priorityWeight;
    }
}
