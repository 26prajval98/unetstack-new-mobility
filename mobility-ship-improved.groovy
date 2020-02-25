import org.arl.fjage.*
import static org.arl.unet.Services.*

platform = RealTimePlatform

def calculate_distance = {
    x_1, y_1, x_2, y_2 ->
    return Math.sqrt((x_2-x_1)**2 + (y_2-y_1)**2)
}

def rotate_axis_xy(location, theta){
    def n_l = []
    n_l << (location[0]*Math.cos(theta) + location[1]*Math.sin(theta))
    n_l << (location[1]*Math.cos(theta) - location[0]*Math.sin(theta))
    n_l << location[2]
    return n_l
}

def rotate_axis_x_y(location, theta){
    def n_l = []
    n_l << (location[0]*Math.cos(theta) - location[1]*Math.sin(theta))
    n_l << (location[1]*Math.cos(theta) + location[0]*Math.sin(theta))
    n_l << location[2]
    return n_l
}

def s_1 = {
    location, time_increment, time, params ->
    
    f_l = []
    def theta_r = Math.toRadians(params['theta'])
    
    location = rotate_axis_xy(location, theta_r)
    
    def (c1, c2, c3, c4, c5, c6) = [0.5, 0.6, 0.2, 0.3, 0.5, 1.5]
    def d = calculate_distance(location[0], location[1], params['x_s'], params['y_s']) 
    
    def v_o = (c2*params['v_s']-c1*location[0])*c6*Math.exp(-c3*d)
    
    f_l << location[0] + c4*Math.random()*Math.exp(-c5*d)
    f_l << location[1] + v_o*time_increment
    f_l << location[2]
    
    params['y_s'] = params['y_s'] + params['v_s']*time_increment*Math.sin(theta_r)
    params['x_s'] = params['x_s'] + params['v_s']*time_increment*Math.cos(theta_r)

    f_l = rotate_axis_x_y(f_l, theta_r)

    return [f_l, params]
}

def generateMotionArray(initial_loc, final_loc, time, time_increment){
    distance = 0
    final_loc.eachWithIndex{
        item, index ->
        distance += ((initial_loc[index] - item) * (initial_loc[index] - item))
    }
    
    distance = Math.sqrt(distance)
    speed = distance / time_increment
    
    if(final_loc[0] == initial_loc[0] && final_loc[1] == initial_loc[1] && final_loc[2] == initial_loc[2]){
        heading = 90
        diveRate = 0
    }
    else if(final_loc[0] == initial_loc[0]){
        heading = 90        
        diveRate = (final_loc[2] - initial_loc[2])/time_increment
    }
    else{
        heading = 90 - Math.toDegrees(Math.atan((final_loc[1] - initial_loc[1])/(final_loc[0] - initial_loc[0])))
        diveRate = (final_loc[2] - initial_loc[2])/time_increment
    }
    
    return [time: time, speed: speed, heading: heading, diveRate: diveRate]
}

def combine_all_scenarios(location, scenarios, simulation_time = 10.minutes, time_increment = 1.seconds){
    f_l = location
    i_l = location
    motionArray = []
    def time = 0.seconds
    while(time < simulation_time){
        time += time_increment
        scenarios.each{
            scenario ->
                def params = []
                if(scenario.containsKey('params'))
                    params = scenario['params']
                vals = scenario['scenario'](f_l, time_increment, time, scenario['params'])
                f_l = vals[0]
                scenario['params'] = vals[1]
        }
        motionArray << generateMotionArray(i_l, f_l, time, time_increment)
        i_l = f_l
    }
    // println(motionArray)
    return motionArray
}

def m_node(name, location, mobility, scenarios){
    def n = node(name, location: location, mobility: mobility)
    n.motionModel = combine_all_scenarios(location, scenarios)
}

simulate 480.seconds, {
    def model_n_1 = [
            [
                'scenario' : s_1, 
                'params' : [
                        'v_s' : 3,
                        'x_s' : 0,
                        'y_s' : 0,
                        'theta' : -45,
                    ],
            ],
        ]
        
    def n_1 = m_node('AUV-1', [0.m, 0.m, 0.m], true, model_n_1)
    // def n_2 = m_node('AUV-1', [4.m, 0.m, 0.m], true, model_n_1)    
    // def n_3 = m_node('AUV-1', [25.m, 0.m, 0.m], true, model_n_1)
}
