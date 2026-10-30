import argparse, json
from pathlib import Path
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.compose import ColumnTransformer
from sklearn.pipeline import Pipeline
from sklearn.impute import SimpleImputer
from sklearn.preprocessing import OneHotEncoder, StandardScaler
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import precision_score, recall_score, f1_score, average_precision_score, confusion_matrix

parser=argparse.ArgumentParser()
parser.add_argument("csv")
parser.add_argument("--output",default="ml/results.json")
a=parser.parse_args()
df=pd.read_csv(a.csv)
y=df["is_fraud"]
X=df.drop(columns=["is_fraud","transaction_id","account_id"])
train,test,yt,yv=train_test_split(X,y,test_size=.2,stratify=y,random_state=42)
num=X.select_dtypes(include="number").columns.tolist()
cat=[c for c in X.columns if c not in num]
prep=ColumnTransformer([("num",Pipeline([("impute",SimpleImputer(strategy="median")),("scale",StandardScaler())]),num),("cat",Pipeline([("impute",SimpleImputer(strategy="most_frequent")),("encode",OneHotEncoder(handle_unknown="ignore"))]),cat)])
model=Pipeline([("preprocess",prep),("model",LogisticRegression(class_weight="balanced",max_iter=1000,random_state=42))])
model.fit(train,yt)
scores=model.predict_proba(test)[:,1]
pred=(scores>=.5).astype(int)
result={"rows":len(df),"fraud_rate":float(y.mean()),"missing":df.isna().sum().to_dict(),"train_rows":len(train),"test_rows":len(test),"threshold":.5,"precision":precision_score(yv,pred,zero_division=0),"recall":recall_score(yv,pred,zero_division=0),"f1":f1_score(yv,pred,zero_division=0),"average_precision":average_precision_score(yv,scores),"confusion_matrix":confusion_matrix(yv,pred,labels=[0,1]).tolist()}
Path(a.output).parent.mkdir(parents=True,exist_ok=True)
Path(a.output).write_text(json.dumps(result,indent=2),encoding="utf-8")
print(json.dumps(result,indent=2))
