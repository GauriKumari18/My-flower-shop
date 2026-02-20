import pandas as pd
from datetime import datetime, timedelta

def get_recommendations(user_id, df_orders, df_order_items, df_flowers):
    recommendations = {
        "most_purchased": [],
        "seasonal": [],
        "trending": []
    }

    if df_orders.empty or df_flowers.empty:
        return recommendations

    # 1. Most Purchased by this user
    user_orders = df_orders[df_orders['user_id'] == user_id]
    if not user_orders.empty:
        user_items = df_order_items[df_order_items['order_id'].isin(user_orders['id'])]
        if not user_items.empty:
            top_flower_ids = user_items.groupby('flower_id')['quantity'].sum().sort_values(ascending=False).head(3).index.tolist()
            recommendations["most_purchased"] = df_flowers[df_flowers['id'].isin(top_flower_ids)][['id', 'name', 'price']].to_dict('records')

    # 2. Recently Trending (Global top sales in last 7 days)
    last_7_days = datetime.now() - timedelta(days=7)
    recent_orders = df_orders[pd.to_datetime(df_orders['ordered_at']) >= last_7_days]
    if not recent_orders.empty:
        recent_items = df_order_items[df_order_items['order_id'].isin(recent_orders['id'])]
        if not recent_items.empty:
            trending_flower_ids = recent_items.groupby('flower_id')['quantity'].sum().sort_values(ascending=False).head(3).index.tolist()
            recommendations["trending"] = df_flowers[df_flowers['id'].isin(trending_flower_ids)][['id', 'name', 'price']].to_dict('records')

    # 3. Seasonal Flowers (Simple Logic)
    # Mapping months to typical seasonal flowers
    month = datetime.now().month
    seasonal_map = {
        (12, 1, 2): ["Poinsettia", "Winter Jasmine", "Amaryllis"],
        (3, 4, 5): ["Tulip", "Daffodil", "Peony"],
        (6, 7, 8): ["Sunflower", "Rose", "Lavender"],
        (9, 10, 11): ["Chrysanthemum", "Marigold", "Dahlia"]
    }
    
    current_season_flowers = []
    for months, flowers in seasonal_map.items():
        if month in months:
            current_season_flowers = flowers
            break
    
    # Matching seasonal names with catalog
    recommendations["seasonal"] = df_flowers[df_flowers['name'].str.contains('|'.join(current_season_flowers), case=False, na=False)].to_dict('records')

    return recommendations
