import java.util.List;

public class Calculate {
	
	public double[] calculateValues(List<String> list) {
		double max=Double.parseDouble(list.get(0)), min=Double.parseDouble(list.get(0)), mean=0, sd=0;
		
		for(String element: list) {
			double currentvalue= Double.parseDouble(element);
			if(currentvalue>max) {
				max=currentvalue;
			}
			else if (currentvalue<min) {
				min=currentvalue;
			}
			mean+=currentvalue;
		}
		mean/=list.size();
		double temp=0;
		for(String element: list) {
			temp+=Math.pow((Double.parseDouble(element)-mean),2);
		}
		sd=Math.sqrt(temp/list.size());
		
		return new double[] {min,max,mean, sd};
		
	}
	

}
