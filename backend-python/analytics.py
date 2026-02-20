import pandas as pd
from datetime import datetime, timedelta
from sklearn.linear_model import LinearRegression
import numpy as np

def get_sales_analytics(df_orders, df_order_items, df_flowers):
    if df_orders.empty or df_order_items.empty:
        return {
            "most_sold_flower": None,
            "daily_revenue": 0,
            "monthly_revenue": 0,
            "top_customers": []
        }

    # Merge data
    df = df_order_items.merge(df_flowers, left_on='flower_id', right_on='id')
    df = df.merge(df_orders, left_on='order_id', right_on='id')

    # Convert ordered_at to datetime
    df['ordered_at'] = pd.to_datetime(df['ordered_at'])

    # 1. Most Sold Flower
    most_sold = df.groupby('name')['quantity'].sum().idxmax() if not df.empty else None

    # 2. Daily Revenue (Today)
    today = datetime.now().date()
    daily_rev = df[df['ordered_at'].dt.date == today]['total_price'].sum() if not df.empty else 0

    # 3. Monthly Revenue (This Month)
    this_month = datetime.now().month
    this_year = datetime.now().year
    monthly_rev = df[(df['ordered_at'].dt.month == this_month) & (df['ordered_at'].dt.year == this_year)]['total_price'].sum() if not df.empty else 0

    # 4. Top Customers
    top_customers = df.groupby('user_id')['total_price'].sum().sort_values(ascending=False).head(5).to_dict()

    return {
        "most_sold_flower": most_sold,
        "daily_revenue": float(daily_rev),
        "monthly_revenue": float(monthly_rev),
        "top_customers": top_customers
    }

def predict_demand(df_orders):
    if df_orders.empty:
        return []

    df_orders['ordered_at'] = pd.to_datetime(df_orders['ordered_at'])
    daily_sales = df_orders.groupby(df_orders['ordered_at'].dt.date)['total_price'].sum().reset_index()
    daily_sales.columns = ['ds', 'y']

    if len(daily_sales) < 2:
        return []

    # Simple Linear Regression
    daily_sales['day_num'] = np.arange(len(daily_sales))
    X = daily_sales[['day_num']]
    y = daily_sales['y']

    model = LinearRegression()
    model.fit(X, y)

    # Predict next 7 days
    future_days = np.array([len(daily_sales) + i for i in range(7)]).reshape(-1, 1)
    predictions = model.predict(future_days)

    forecast = []
    last_date = daily_sales['ds'].max()
    for i, pred in enumerate(predictions):
        forecast_date = last_date + timedelta(days=i+1)
        forecast.append({
            "date": forecast_date.strftime('%Y-%m-%d'),
            "predicted_demand": max(0, float(pred))
        })

    return forecast
