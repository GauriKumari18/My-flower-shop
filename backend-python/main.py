from fastapi import FastAPI, HTTPException
from sqlalchemy import create_engine
import pandas as pd
import recommender as recommender_logic
import analytics as analytics_logic
import os

app = FastAPI(title="Flower Shop AI & Analytics API")

# Database configuration (matching Java backend)
DB_URL = "postgresql://Gauri:gauri@localhost:5432/flower_shop_db"
engine = create_engine(DB_URL)

def get_dataframes():
    try:
        df_orders = pd.read_sql("SELECT * FROM orders", engine)
        df_order_items = pd.read_sql("SELECT * FROM order_items", engine)
        df_flowers = pd.read_sql("SELECT * FROM flowers", engine)
        return df_orders, df_order_items, df_flowers
    except Exception as e:
        print(f"Error fetching data: {e}")
        return pd.DataFrame(), pd.DataFrame(), pd.DataFrame()

@app.get("/recommend/{user_id}")
async def recommend(user_id: int):
    df_orders, df_order_items, df_flowers = get_dataframes()
    if df_flowers.empty:
        raise HTTPException(status_code=500, detail="Database connection failed or tables empty")
    
    recs = recommender_logic.get_recommendations(user_id, df_orders, df_order_items, df_flowers)
    return recs

@app.get("/analytics/sales")
async def sales_analytics():
    df_orders, df_order_items, df_flowers = get_dataframes()
    if df_flowers.empty:
        raise HTTPException(status_code=500, detail="Database connection failed or tables empty")
    
    analytics = analytics_logic.get_sales_analytics(df_orders, df_order_items, df_flowers)
    return analytics

@app.get("/analytics/demand-prediction")
async def demand_prediction():
    df_orders, _, _ = get_dataframes()
    if df_orders.empty:
        raise HTTPException(status_code=500, detail="No order data available for prediction")
    
    prediction = analytics_logic.predict_demand(df_orders)
    return prediction

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
